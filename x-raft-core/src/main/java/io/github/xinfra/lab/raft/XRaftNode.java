package io.github.xinfra.lab.raft;

import io.github.xinfra.lab.raft.transport.RaftServerTransportFactory;
import lombok.Getter;

import java.util.concurrent.ThreadLocalRandom;

public class XRaftNode extends AbstractLifeCycle implements RaftNode {

	private RaftPeer raftPeer;

	private RaftGroup raftGroup;

	private RaftNodeConfig raftNodeConfig;

	@Getter
	private RaftNodeState state;

	@Getter
	private RaftLog raftLog;

	@Getter
	private RaftServerTransport raftServerTransport;

	public XRaftNode(RaftPeer raftPeer, RaftGroup raftGroup, RaftNodeConfig raftNodeConfig) {
		this.raftPeer = raftPeer;
		this.raftGroup = raftGroup;
		this.raftNodeConfig = raftNodeConfig;
		this.state = new RaftNodeState(this);
		this.raftServerTransport = RaftServerTransportFactory.create(raftNodeConfig.getTransportType(), this);
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
		raftServerTransport.addRaftPeers(state.getRaftConfiguration().getRaftPeers());
		changeToFollower();
	}

	@Override
	public void shutdown() {
		super.shutdown();

	}

	@Override
	public VoteResponse requestVote(VoteRequest voteRequest) {
		// todo
		return null;
	}

	public Long getRandomElectionTimeoutMills() {
		Long min = raftNodeConfig.getMinRpcTimeoutMills();
		Long max = raftNodeConfig.getMaxRpcTimeoutMills();
		return min + ThreadLocalRandom.current().nextLong(max - min) + 1;
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

	@Override
	public SetConfigurationResponse setRaftConfiguration(SetConfigurationRequest request) {
		// todo
		return null;
	}

}
