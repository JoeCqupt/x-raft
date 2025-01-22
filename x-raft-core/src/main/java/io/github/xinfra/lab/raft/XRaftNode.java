package io.github.xinfra.lab.raft;

import io.github.xinfra.lab.raft.transport.RaftServerTransportFactory;
import lombok.Getter;

public class XRaftNode extends AbstractLifeCycle implements RaftNode {

	private RaftPeer raftPeer;

	private RaftGroup raftGroup;

	private RaftNodeConfig config;
	@Getter
	private RaftNodeState state;

	@Getter
	private RaftLog raftLog;

	@Getter
	private RaftServerTransport raftServerTransport;

	public XRaftNode(RaftPeer raftPeer, RaftGroup raftGroup, RaftNodeConfig config) {
		this.raftPeer = raftPeer;
		this.raftGroup = raftGroup;
		this.config = config;
		this.state = new RaftNodeState(this);
		this.raftServerTransport = RaftServerTransportFactory.create(config.getTransportType(), this);
	}

	@Override
	public RaftPeer self() {
		return raftPeer;
	}

	@Override
	public RaftGroup getRaftGroup() {
		return raftGroup;
	}

	@Override
	public void startup() {
		super.startup();
		raftServerTransport.startup();
		raftServerTransport.addRaftPeers(raftGroup.getRaftPeers());
		changeToFollower();
	}

	@Override
	public void shutdown() {
		super.shutdown();

	}

	@Override
	public RequestVoteResponse requestVote(RequestVoteRequest requestVoteRequest) {
		// todo
		return null;
	}

	public Long getRandomElectionTimeout() {
		// todo
		return null;
	}

	private synchronized void changeToFollower() {
		if (state.getRole() == RaftRole.CANDIDATE) {
			state.shutdownCandidateState();
		}
		if (state.getRole() == RaftRole.LEADER) {
			state.shutdownLeaderState();
		}
		state.startFollowerState();
	}

	public synchronized void changeToCandidate() {
		state.shutdownFollowerState();
		state.startCandidateState();
	}

	public synchronized void changeToLeader() {
		state.shutdownCandidateState();
		state.startLeaderState();
	}

}
