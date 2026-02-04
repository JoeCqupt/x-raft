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

@Slf4j
public class CandidateState {

	private volatile boolean running;

	private final XRaftNode xRaftNode;

	private Thread electionTask;

	private BallotBox voteBallotBox;

	public CandidateState(XRaftNode xRaftNode) {
		this.xRaftNode = xRaftNode;
	}

	public void startup() {
		if (running) {
			return;
		}
		running = true;
		electionTask = new ElectionTaskThread();
		electionTask.start();
	}

	public void shutdown() {
		if (!running) {
			return;
		}
		running = false;
		electionTask.interrupt();
		electionTask = null;
	}

	class ElectionTaskThread extends Thread {

		public ElectionTaskThread() {
			super("ElectionTask");
		}

		@Override
		public void run() {
			while (shouldRun()) {
				try {
					try {
						xRaftNode.getState().getWriteLock().lock();
						if (xRaftNode.getState().getRole() != RaftRole.CANDIDATE) {
							log.info("ElectionTaskThread exist. current role:{}", xRaftNode.getState().getRole());
							break;
						}
						vote();
					}
					finally {
						xRaftNode.getState().getWriteLock().unlock();
					}
					Thread.sleep(xRaftNode.getRaftNodeOptions().getRandomElectionTimeoutMills());
				}
				catch (InterruptedException e) {
					log.info("ElectionTaskThread interrupted");
					Thread.currentThread().interrupt();
					break;
				}
				catch (Throwable t) {
					log.error("ElectionTaskThread run ex", t);
				}
			}
		}

		private boolean shouldRun() {
			return running && !Thread.currentThread().isInterrupted();
		}

	}

	private void vote() throws Exception {
		log.info("node:{} ask for votes", xRaftNode.getRaftGroupPeerId());
		xRaftNode.getState().resetLeaderId(null);

		ConfigurationEntry config = xRaftNode.getState().getConfigState().getCurrentConfig();
		if (config.getRaftPeer(xRaftNode.getRaftPeerId()) == null) {
			log.warn("node:{} is not in the raft group", xRaftNode.getRaftGroupPeerId());
			return;
		}

		// update current term and votedFor
		// 先持久化,成功后再继续选举流程,避免持久化失败导致状态不一致
		long electionTerm = xRaftNode.getState().getCurrentTerm() + 1;
		long oldTerm = xRaftNode.getState().getCurrentTerm();
		String oldVotedFor = xRaftNode.getState().getVotedFor();

		xRaftNode.getState().setCurrentTerm(electionTerm);
		xRaftNode.getState().setVotedFor(xRaftNode.getRaftPeerId());

		try {
			xRaftNode.getState().persistMetadata();
		}
		catch (Exception e) {
			// 持久化失败,回滚状态并退出选举
			log.error("Failed to persist metadata during election, rolling back term from {} to {}", electionTerm,
					oldTerm, e);
			xRaftNode.getState().setCurrentTerm(oldTerm);
			xRaftNode.getState().setVotedFor(oldVotedFor);
			return;
		}

		TermIndex lastLogIndex = xRaftNode.getState().getRaftLog().getLastEntryTermIndex();

		voteBallotBox = new BallotBox(config);

		CallOptions callOptions = new CallOptions();
		callOptions.setTimeoutMs(xRaftNode.getRaftNodeOptions().getElectionTimeoutMills());
		for (RaftPeer raftPeer : config.getPeers()) {
			if (xRaftNode.getRaftPeerId().equals(raftPeer.getRaftPeerId())) {
				continue;
			}
			VoteRequest voteRequest = new VoteRequest();
			voteRequest.setRaftGroupId(xRaftNode.getRaftGroupId());
			voteRequest.setRaftPeerId(raftPeer.getRaftPeerId());
			voteRequest.setPreVote(false);
			voteRequest.setTerm(electionTerm);
			voteRequest.setCandidateId(xRaftNode.getRaftPeerId());
			voteRequest.setLastLogIndex(lastLogIndex.getIndex());
			voteRequest.setLastLogTerm(lastLogIndex.getTerm());

			log.info("node:{} send vote to {}", xRaftNode.getRaftGroupPeerId(), raftPeer);
			VoteResponseCallBack callBack = new VoteResponseCallBack(electionTerm, raftPeer, voteBallotBox);
			xRaftNode.getTransportClient()
				.asyncCall(RaftApi.requestVote, voteRequest, raftPeer.getAddress(), callOptions, callBack);
		}
		// grant self vote
		voteBallotBox.grantVote(xRaftNode.getRaftPeerId());
		if (voteBallotBox.isMajorityGranted()) {
			xRaftNode.getState().changeToLeader();
		}
	}

	class VoteResponseCallBack implements ResponseCallBack<VoteResponse> {

		private final long term;

		private final RaftPeer raftPeer;

		private final BallotBox ballotBox;

		public VoteResponseCallBack(long term, RaftPeer raftPeer, BallotBox ballotBox) {
			this.term = term;
			this.raftPeer = raftPeer;
			this.ballotBox = ballotBox;
		}

		@Override
		public void onResponse(VoteResponse response) {
			try {
				xRaftNode.getState().getWriteLock().lock();
				if (xRaftNode.getState().getRole() != RaftRole.CANDIDATE) {
					log.warn("raftPeer:{} VoteResponseCallBack exist, current role is {}", raftPeer,
							xRaftNode.getState().getRole());
					return;
				}
				if (term != xRaftNode.getState().getCurrentTerm()) {
					log.warn("raftPeer:{} VoteResponseCallBack is outdated", raftPeer);
					return;
				}
				if (ballotBox != voteBallotBox) {
					log.warn("raftPeer:{} VoteResponseCallBack is outdated", raftPeer);
					return;
				}
				if (response.getTerm() > xRaftNode.getState().getCurrentTerm()) {
					log.warn("raftPeer:{} VoteResponseCallBack response term is newer:{}", raftPeer,
							response.getTerm());
					xRaftNode.getState().changeToFollower(response.getTerm());
					return;
				}
				if (response.isVoteGranted()) {
					log.info("raftPeer:{} VoteResponseCallBack response granted", raftPeer);
					ballotBox.grantVote(raftPeer.getRaftPeerId());
					if (ballotBox.isMajorityGranted()) {
						log.info("raftPeer:{} VoteResponseCallBack majority granted. change to leader", raftPeer);
						xRaftNode.getState().changeToLeader();
					}
				}
			}
			finally {
				xRaftNode.getState().getWriteLock().unlock();
			}
		}

		@Override
		public void onException(Throwable throwable) {
			log.error("raftPeer:{} VoteResponseCallBack error", raftPeer, throwable);
		}

	}

}
