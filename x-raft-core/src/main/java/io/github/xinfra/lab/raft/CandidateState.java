package io.github.xinfra.lab.raft;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;

@Slf4j
public class CandidateState extends Thread {

	private volatile boolean running = true;

	private final XRaftNode xRaftNode;

	public CandidateState(XRaftNode xRaftNode) {
		this.xRaftNode = xRaftNode;
	}

	@Override
	public void run() {
		while (shouldRun()) {
			try {
				if (preVote()) {
					if (vote()) {
						synchronized (xRaftNode) {
							if (shouldRun()) {
								xRaftNode.changeToLeader();
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

	private boolean vote() {
		// todo
		return false;
	}

	private boolean preVote() {
		long electionTerm;
		RaftConfiguration raftConfiguration;
		TermIndex lastEntryTermIndex;
		synchronized (xRaftNode) {
			if (!shouldRun()) {
				return false;
			}
			electionTerm = xRaftNode.getState().getCurrentTerm().get();
			raftConfiguration = xRaftNode.getState().getRaftConfiguration();
			lastEntryTermIndex = xRaftNode.getRaftLog().getLastEntryTermIndex();
		}

		VoteResult voteResult = sendVoteRequests(true, electionTerm, raftConfiguration, lastEntryTermIndex);
		return false;
	}

	private VoteResult sendVoteRequests(boolean preVote, long electionTerm, RaftConfiguration raftConfiguration,
			TermIndex lastEntryTermIndex) {
		if (!(raftConfiguration.getConf().getVotingPeers().contains(xRaftNode.self()))) {
			return new VoteResult(electionTerm, Status.NOT_IN_CONF);
		}

		Set<RaftPeer> otherRaftPeers = raftConfiguration.getVotingRaftPeers();
		if (otherRaftPeers.isEmpty()) {
			return new VoteResult(electionTerm, Status.PASSED);
		}

		// init ballot box
		BallotBox ballotBox = new BallotBox(raftConfiguration);
		// vote to self
		ballotBox.grantVote(xRaftNode.self());

		ExecutorCompletionService<VoteResponse> voteExecutor = new ExecutorCompletionService<>(
				Executors.newFixedThreadPool(otherRaftPeers.size()));
		for (RaftPeer raftPeer : otherRaftPeers) {

			// build request
			VoteRequest voteRequest = new VoteRequest();
			voteRequest.setPreVote(preVote);
			voteRequest.setCandidateId(xRaftNode.self().getRaftPeerId());
			voteRequest.setTerm(electionTerm);
			voteRequest.setLastLogIndex(lastEntryTermIndex.getIndex());
			voteRequest.setLastLogTerm(lastEntryTermIndex.getTerm());
			voteRequest.setRequestPeerId(xRaftNode.self().getRaftPeerId());
			voteRequest.setReplyPeerId(raftPeer.getRaftPeerId());

			voteExecutor.submit(() -> xRaftNode.getRaftServerTransport().requestVote(voteRequest));
		}

		int waitNum = otherRaftPeers.size();
		while (waitNum > 0 && shouldRun()) {

		}

		// todo
		return 	null;
	}

	private boolean shouldRun() {
		return running && xRaftNode.getState().getRole() == RaftRole.CANDIDATE;
	}

	public void shutdown() {
		running = false;
	}

	enum Status {

		PASSED, REJECTED, TIMEOUT, NEW_TERM, SHUTDOWN, NOT_IN_CONF

	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	static class VoteResult {

		private long term;

		private Status status;

	}

}
