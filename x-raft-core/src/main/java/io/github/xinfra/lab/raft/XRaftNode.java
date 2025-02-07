package io.github.xinfra.lab.raft;

import io.github.xinfra.lab.raft.transport.RaftServerTransportFactory;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.ThreadLocalRandom;

public class XRaftNode extends AbstractLifeCycle implements RaftNode {

	private RaftPeer raftPeer;

	private RaftGroup raftGroup;

	private RaftNodeConfig raftNodeConfig;

	@Getter
	private RaftNodeState state;

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
	public synchronized void startup() {
		super.startup();
		// todo: init raft storage
		// todo: init raft log
		// todo: init state machine
		raftServerTransport.startup();
		raftServerTransport.addRaftPeers(state.getRaftConfiguration().getRaftPeers());
		// todo: start role by config
		changeToFollower();
	}

	@Override
	public synchronized void shutdown() {
		super.shutdown();
	}

	@Override
	public VoteResponse requestVote(VoteRequest voteRequest) {
		// todo
		return null;
	}

	@Override
	public AppendEntriesResponse appendEntries(AppendEntriesRequest appendEntriesRequest) {
		// todo
		return null;
	}

	@Override
	public SetConfigurationResponse setRaftConfiguration(SetConfigurationRequest request) {
		// todo
		return null;
	}

	public Long getRandomElectionTimeoutMills() {
		Long min = raftNodeConfig.getMinRpcTimeoutMills();
		Long max = raftNodeConfig.getMaxRpcTimeoutMills();
		return min + ThreadLocalRandom.current().nextLong(max - min) + 1;
	}

	public synchronized void changeToFollower() {
		if (state.getRole() == RaftRole.CANDIDATE) {
			state.shutdownCandidateState();
		}
		if (state.getRole() == RaftRole.LEADER) {
			state.shutdownLeaderState();
		}
		state.startFollowerState();
	}

	/**
	 * new term discovered, change to follower
	 * @param newTerm
	 */
	public synchronized void changeToFollower(long newTerm) {
		// todo
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
