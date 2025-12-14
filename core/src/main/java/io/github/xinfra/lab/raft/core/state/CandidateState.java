package io.github.xinfra.lab.raft.core.state;

import io.github.xinfra.lab.raft.RaftPeerId;
import io.github.xinfra.lab.raft.RaftRole;
import io.github.xinfra.lab.raft.core.XRaftNode;
import io.github.xinfra.lab.raft.core.conf.ConfigurationEntry;
import io.github.xinfra.lab.raft.log.TermIndex;
import io.github.xinfra.lab.raft.protocol.VoteRequest;
import io.github.xinfra.lab.raft.protocol.VoteResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CandidateState extends Thread {

	private volatile boolean running;

	private final XRaftNode xRaftNode;

	private Thread electionTask;

	public CandidateState(XRaftNode xRaftNode) {
		this.xRaftNode = xRaftNode;
	}

	public synchronized void startup() {
		if (running) {
			return;
		}
		running = true;
		electionTask = new ElectionTask();
		electionTask.start();
	}

	public synchronized void shutdown() {
		if (!running) {
			return;
		}
		running = false;
		electionTask.interrupt();
	}

	class ElectionTask extends Thread {

		public ElectionTask() {
			super("ElectionTask");
		}

		@Override
		public void run() {
			while (shouldRun()) {
				try {
					if (askForVotes(true)) {
						if (askForVotes(false)) {
							synchronized (xRaftNode) {
								if (shouldRun()) {
									xRaftNode.getState().changeToLeader();
									break;
								}
							}
						}
					}
				}
				catch (Throwable t) {
					log.error("CandidateState error", t);
				}
			}
		}

		private boolean askForVotes(boolean preVote) throws InterruptedException {
			Long electionTerm;
			ConfigurationEntry configurationEntry;
			TermIndex lastEntryTermIndex;
			synchronized (xRaftNode) {
				if (!shouldRun()) {
					return false;
				}
				// todo: notify state machine
				xRaftNode.getState().getLeaderId().getAndSet(null);
				if (preVote) {
					electionTerm = xRaftNode.getState().getCurrentTerm().get();
				}
				else {
					electionTerm = xRaftNode.getState().getCurrentTerm().incrementAndGet();
					xRaftNode.getState().getVotedFor().getAndSet(xRaftNode.raftPeerId().getPeerId());
					xRaftNode.getState().persistMetadata();
				}
				configurationEntry = xRaftNode.getState().getRaftConfiguration();
				lastEntryTermIndex = xRaftNode.raftLog().getLastEntryTermIndex();
			}

			VoteResult voteResult = askForVotes(preVote, electionTerm, configurationEntry, lastEntryTermIndex);

			synchronized (xRaftNode) {
				if (!shouldRun()) {
					return false;
				}
			}

			switch (voteResult.getStatus()) {
				case PASSED:
					return true;
				case SHUTDOWN:
				case NOT_IN_CONF:
					// todo notify state machine
					xRaftNode.shutdown();
					return false;
				case TIMEOUT:
					return false; // retry
				case NEW_TERM:
					xRaftNode.getState().changeToFollower(voteResult.getTerm());
					return false;
				case REJECTED:
					xRaftNode.getState().changeToFollower();
					return false;
				default:
					throw new IllegalArgumentException("Unable to handle vote result " + voteResult);
			}
		}

		private VoteResult askForVotes(boolean preVote, Long electionTerm, ConfigurationEntry configurationEntry,
				TermIndex lastEntryTermIndex) throws InterruptedException {
			if (!(configurationEntry.getVotingRaftPeers().contains(xRaftNode.raftPeerId()))) {
				return new VoteResult(electionTerm, Status.NOT_IN_CONF);
			}

			Set<RaftPeerId> otherVotingRaftPeerIds = configurationEntry.getOtherVotingRaftPeers();
			if (otherVotingRaftPeerIds.isEmpty()) {
				return new VoteResult(electionTerm, Status.PASSED);
			}

			// init ballot box
			BallotBox ballotBox = new BallotBox(configurationEntry);
			// vote to raftPeerId
			ballotBox.grantVote(xRaftNode.raftPeerId().getPeerId());

			// todo: close it
			ExecutorCompletionService<VoteResponse> voteExecutor = new ExecutorCompletionService<>(
					Executors.newFixedThreadPool(otherVotingRaftPeerIds.size()));
			for (RaftPeerId raftPeerId : otherVotingRaftPeerIds) {
				// build request
				VoteRequest voteRequest = new VoteRequest();
				voteRequest.setPreVote(preVote);
				voteRequest.setCandidateId(xRaftNode.raftPeerId().getPeerId());
				voteRequest.setTerm(electionTerm);
				voteRequest.setLastLogIndex(lastEntryTermIndex.getIndex());
				voteRequest.setLastLogTerm(lastEntryTermIndex.getTerm());
				voteRequest.setRequestPeerId(xRaftNode.raftPeerId().getPeerId());
				voteRequest.setReplyPeerId(raftPeerId.getPeerId());

				voteExecutor.submit(() -> xRaftNode.getRaftServerTransport().requestVote(voteRequest));
			}

			int waitNum = otherVotingRaftPeerIds.size();
			Long electionEndTimeMills = System.currentTimeMillis() + xRaftNode.getRandomElectionTimeoutMills();

			while (waitNum > 0 && shouldRun()) {
				Long leftTimeMills = electionEndTimeMills - System.currentTimeMillis();
				if (leftTimeMills <= 0 && !ballotBox.isMajorityGranted()) {
					return new VoteResult(electionTerm, Status.TIMEOUT);
				}
				else if (leftTimeMills <= 0 && ballotBox.isMajorityGranted()) {
					return new VoteResult(electionTerm, Status.PASSED);
				}

				Future<VoteResponse> responseFuture = voteExecutor.poll(leftTimeMills, TimeUnit.MILLISECONDS);
				if (responseFuture == null) {
					// timeout
					continue;
				}

				VoteResponse voteResponse = null;
				try {
					voteResponse = responseFuture.get();

					if (voteResponse.isShouldShutdown()) {
						return new VoteResult(electionTerm, Status.SHUTDOWN);
					}

					if (voteResponse.getTerm() > electionTerm) {
						return new VoteResult(voteResponse.getTerm(), Status.NEW_TERM);
					}

					if (voteResponse.isVoteGranted()) {
						ballotBox.grantVote(voteResponse.getReplyPeerId());
						if (ballotBox.isMajorityGranted()) {
							return new VoteResult(electionTerm, Status.PASSED);
						}
					}
					else {
						ballotBox.rejectVote(voteResponse.getReplyPeerId());
						if (ballotBox.isMajorityRejected()) {
							return new VoteResult(electionTerm, Status.REJECTED);
						}
					}

				}
				catch (ExecutionException e) {
					log.error("get vote response error", e);
				}

				waitNum--;
			}

			// received all vote response
			if (ballotBox.isMajorityGranted()) {
				return new VoteResult(electionTerm, Status.PASSED);
			}
			else {
				return new VoteResult(electionTerm, Status.REJECTED);
			}
		}

		private boolean shouldRun() {
			return running && xRaftNode.getState().getRole() == RaftRole.CANDIDATE;
		}

	}

	enum Status {

		PASSED, REJECTED, TIMEOUT, NEW_TERM, SHUTDOWN, NOT_IN_CONF

	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	static class VoteResult {

		private Long term;

		private Status status;

	}

}
