package io.github.xinfra.lab.raft;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
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

	private VoteResult sendVoteRequests(boolean preVote,
								  long electionTerm,
								  RaftConfiguration raftConfiguration,
								  TermIndex lastEntryTermIndex) {
		if ((raftConfiguration.))

		Set<RaftPeer> otherRaftPeers = raftConfiguration.getOtherRaftPeers();

		if (otherRaftPeers.isEmpty()) {
			// todo
			return;
		}

		// vote to self
		BallotBox ballotBox = new BallotBox(xRaftNode.getState());
		ballotBox.grantVote(xRaftNode.self());


		ExecutorCompletionService<RequestVoteResponse> voteExecutor = new ExecutorCompletionService<>(
				Executors.newFixedThreadPool(otherRaftPeers.size()));
		for (RaftPeer raftPeer : otherRaftPeers) {

			// build request
			RequestVoteRequest requestVoteRequest = new RequestVoteRequest();
			requestVoteRequest.setPreVote(preVote);
			requestVoteRequest.setCandidateId(xRaftNode.self().getRaftPeerId());
			requestVoteRequest.setTerm(electionTerm);
			requestVoteRequest.setLastLogIndex(lastEntryTermIndex.getIndex());
			requestVoteRequest.setLastLogTerm(lastEntryTermIndex.getTerm());
			requestVoteRequest.setRequestPeerId(xRaftNode.self().getRaftPeerId());
			requestVoteRequest.setReplyPeerId(raftPeer.getRaftPeerId());

			voteExecutor.submit(() -> xRaftNode.getRaftServerTransport().requestVote(requestVoteRequest));
		}

		int waitNum = otherRaftPeers.size();
		while (waitNum > 0 && shouldRun()) {

		}
	}

	private boolean shouldRun() {
		return running && xRaftNode.getState().getRole() == RaftRole.CANDIDATE;
	}

	public void shutdown() {
		running = false;
	}

	enum Status {
		PASSED, REJECTED, TIMEOUT, NEW_TERM, SHUTDOWN
	}
	static class VoteResult {

	}
}
