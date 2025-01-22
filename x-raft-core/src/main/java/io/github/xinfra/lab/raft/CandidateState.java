package io.github.xinfra.lab.raft;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
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
						xRaftNode.changeToLeader();
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
		synchronized (xRaftNode) {
			if (!shouldRun()) {
				return false;
			}

			long term = xRaftNode.getState().getCurrentTerm().get();

		}

		sendVoteRequests(true);

		return false;
	}

	private void sendVoteRequests(boolean preVote) {

		List<RaftPeer> otherPeers = xRaftNode.getState().remoteVotingMembers();
		if (otherPeers.isEmpty()) {
			// todo
			return;
		}

		// vote to self
		BallotBox ballotBox = new BallotBox(xRaftNode.getState());
		ballotBox.grantVote(xRaftNode.self());

		long electionTerm;
		if (preVote) {
			electionTerm = xRaftNode.getState().getCurrentTerm().get();
		}
		else {
			// todo
			electionTerm = 0;
		}

		TermIndex lastEntry = xRaftNode.getRaftLog().getLastEntryTermIndex();

		ExecutorCompletionService<RequestVoteResponse> voteExecutor = new ExecutorCompletionService<>(
				Executors.newFixedThreadPool(otherPeers.size()));
		for (RaftPeer raftPeer : otherPeers) {

			// build request
			RequestVoteRequest requestVoteRequest = new RequestVoteRequest();
			requestVoteRequest.setPreVote(preVote);
			requestVoteRequest.setCandidateId(xRaftNode.self().getRaftPeerId());
			requestVoteRequest.setTerm(electionTerm);
			requestVoteRequest.setLastLogIndex(lastEntry.getIndex());
			requestVoteRequest.setLastLogTerm(lastEntry.getTerm());
			requestVoteRequest.setRequestPeerId(xRaftNode.self().getRaftPeerId());
			requestVoteRequest.setReplyPeerId(raftPeer.getRaftPeerId());

			voteExecutor.submit(() -> xRaftNode.getRaftServerTransport().requestVote(requestVoteRequest));
		}

		int waitNum = otherPeers.size();
		while (waitNum > 0 && shouldRun()) {

		}
	}

	private boolean shouldRun() {
		return running && xRaftNode.getState().getRole() == RaftRole.CANDIDATE;
	}

	public void shutdown() {
		running = false;
	}

}
