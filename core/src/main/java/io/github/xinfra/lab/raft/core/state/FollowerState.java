package io.github.xinfra.lab.raft.core.state;

import io.github.xinfra.lab.raft.RaftPeer;
import io.github.xinfra.lab.raft.RaftRole;
import io.github.xinfra.lab.raft.conf.ConfigurationEntry;
import io.github.xinfra.lab.raft.core.XRaftNode;
import io.github.xinfra.lab.raft.core.transport.RaftApi;
import io.github.xinfra.lab.raft.log.TermIndex;
import io.github.xinfra.lab.raft.protocol.VoteRequest;
import io.github.xinfra.lab.raft.protocol.VoteResponse;
import io.github.xinfra.lab.raft.transport.CallOptions;
import io.github.xinfra.lab.raft.transport.ResponseCallBack;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class FollowerState {

	private final XRaftNode xRaftNode;

	private volatile boolean running;

	private Thread electionTimeoutTask;

	private BallotBox preVoteBallotBox;

	public FollowerState(XRaftNode xRaftNode) {
		this.xRaftNode = xRaftNode;
	}

	public void startup() {
		if (running) {
			return;
		}
		running = true;
		electionTimeoutTask = new ElectionTimeoutTask();
		electionTimeoutTask.start();
	}

	public void shutdown() {
		if (!running) {
			return;
		}
		running = false;
		electionTimeoutTask.interrupt();
		electionTimeoutTask = null; // help gc
	}

	class ElectionTimeoutTask extends Thread {

		public ElectionTimeoutTask() {
			super(String.format("ElectionTimeoutThread-%s", xRaftNode.getRaftGroupPeerId()));
		}

		@Override
		public void run() {
			while (shouldRun()) {
				try {
					Long electionTimeoutMills = xRaftNode.getRaftNodeOptions().getRandomElectionTimeoutMills();
					TimeUnit.MILLISECONDS.sleep(electionTimeoutMills);
					try {
						xRaftNode.getState().getWriteLock().lock();
						if (xRaftNode.getState().getRole() != RaftRole.FOLLOWER) {
							log.info("ElectionTimeoutThread exit, current role is {}", xRaftNode.getState().getRole());
							break;
						}
						if (timeout(electionTimeoutMills)) {
							preVote();
						}
					}
					finally {
						xRaftNode.getState().getWriteLock().unlock();
					}
				}
				catch (InterruptedException e) {
					log.info("ElectionTimeoutThread interrupted");
					Thread.currentThread().interrupt();
					break;
				}
				catch (Throwable t) {
					log.error("ElectionTimeoutThread ex", t);
				}
			}
		}

		public boolean timeout(long electionTimeoutMills) {
			return System.currentTimeMillis()
					- xRaftNode.getState().getLastLeaderRpcTimeMills() >= electionTimeoutMills;
		}

		private boolean shouldRun() {
			return running && !Thread.currentThread().isInterrupted();
		}

	}

	private void preVote() throws Exception {
		log.info("node:{} start preVote", xRaftNode.getRaftGroupPeerId());
		// reset leader
		xRaftNode.getState().resetLeaderId(null);

		ConfigurationEntry config = xRaftNode.getState().getConfigState().getCurrentConfig();
		if (config.getRaftPeer(xRaftNode.getRaftPeerId()) == null) {
			log.warn("node:{} is not in the raft group", xRaftNode.getRaftGroupPeerId());
			return;
		}

		long term = xRaftNode.getState().getCurrentTerm();
		TermIndex lastLogIndex = xRaftNode.getState().getRaftLog().getLastEntryTermIndex();
		preVoteBallotBox = new BallotBox(config);

		CallOptions callOptions = new CallOptions();
		callOptions.setTimeoutMs(xRaftNode.getRaftNodeOptions().getElectionTimeoutMills());
		for (RaftPeer raftPeer : config.getPeers()) {
			if (xRaftNode.getRaftPeerId().equals(raftPeer.getRaftPeerId())) {
				continue;
			}
			VoteRequest voteRequest = new VoteRequest();
			voteRequest.setRaftGroupId(xRaftNode.getRaftGroupId());
			voteRequest.setRaftPeerId(raftPeer.getRaftPeerId());
			voteRequest.setPreVote(true);
			voteRequest.setTerm(term + 1);
			voteRequest.setCandidateId(xRaftNode.getRaftPeerId());
			voteRequest.setLastLogIndex(lastLogIndex.getIndex());
			voteRequest.setLastLogTerm(lastLogIndex.getTerm());

            log.info("node:{} send preVote to {}", xRaftNode.getRaftGroupPeerId(), raftPeer);
			PreVoteResponseCallBack callBack = new PreVoteResponseCallBack(term, raftPeer, preVoteBallotBox);
			xRaftNode.getTransportClient()
				.asyncCall(RaftApi.requestVote, voteRequest, raftPeer.getAddress(), callOptions, callBack);
		}
		// grant self vote
		preVoteBallotBox.grantVote(xRaftNode.getRaftPeerId());
		if (preVoteBallotBox.isMajorityGranted()) {
			xRaftNode.getState().changeToCandidate();
		}
	}

	class PreVoteResponseCallBack implements ResponseCallBack<VoteResponse> {

		private final long term;

		private final RaftPeer raftPeer;

		private final BallotBox ballotBox;

		public PreVoteResponseCallBack(long term, RaftPeer raftPeer, BallotBox ballotBox) {
			this.term = term;
			this.raftPeer = raftPeer;
			this.ballotBox = ballotBox;
		}

		@Override
		public void onResponse(VoteResponse response) {
			try {
				xRaftNode.getState().getWriteLock().lock();
				if (xRaftNode.getState().getRole() != RaftRole.FOLLOWER) {
					log.info("raftPeer:{} PreVoteResponseCallBack exit, current role is {}", raftPeer,
							xRaftNode.getState().getRole());
					return;
				}
				if (term != xRaftNode.getState().getCurrentTerm()) {
					log.warn("raftPeer:{} PreVoteResponseCallBack is outdated", raftPeer);
					return;
				}
				if (ballotBox != preVoteBallotBox) {
					log.warn("raftPeer:{} PreVoteResponseCallBack is outdated", raftPeer);
					return;
				}
				if (response.getTerm() > xRaftNode.getState().getCurrentTerm()) {
					log.warn("raftPeer:{} PreVoteResponseCallBack response term is newer:{}", raftPeer, response.getTerm());
					xRaftNode.getState().changeToFollower(response.getTerm());
					return;
				}
				if (response.isVoteGranted()) {
					log.info("raftPeer:{} PreVoteResponseCallBack vote granted", raftPeer);
					ballotBox.grantVote(raftPeer.getRaftPeerId());
					if (ballotBox.isMajorityGranted()) {
						log.info("raftPeer:{} PreVoteResponseCallBack majority vote granted. change to candidate",
								raftPeer);
						xRaftNode.getState().changeToCandidate();
					}
				}
			}
			finally {
				xRaftNode.getState().getWriteLock().unlock();
			}
		}

		@Override
		public void onException(Throwable throwable) {
			log.warn("raftPeer:{} PreVoteResponseCallBack error:{}", raftPeer, throwable);
		}

	}

}
