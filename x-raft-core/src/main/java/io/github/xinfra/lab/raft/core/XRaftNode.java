package io.github.xinfra.lab.raft.core;

import com.google.common.base.Verify;
import io.github.xinfra.lab.raft.AbstractLifeCycle;
import io.github.xinfra.lab.raft.RaftGroup;
import io.github.xinfra.lab.raft.RaftNode;
import io.github.xinfra.lab.raft.RaftNodeConfig;
import io.github.xinfra.lab.raft.RaftPeer;
import io.github.xinfra.lab.raft.RaftRole;
import io.github.xinfra.lab.raft.core.common.Responses;
import io.github.xinfra.lab.raft.core.state.RaftNodeState;
import io.github.xinfra.lab.raft.log.RaftLog;
import io.github.xinfra.lab.raft.protocol.AppendEntriesRequest;
import io.github.xinfra.lab.raft.protocol.AppendEntriesResponse;
import io.github.xinfra.lab.raft.protocol.SetConfigurationRequest;
import io.github.xinfra.lab.raft.protocol.SetConfigurationResponse;
import io.github.xinfra.lab.raft.protocol.VoteRequest;
import io.github.xinfra.lab.raft.protocol.VoteResponse;
import io.github.xinfra.lab.raft.transport.RaftServerTransport;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class XRaftNode extends AbstractLifeCycle implements RaftNode {

	private RaftPeer raftPeer;

	private RaftGroup raftGroup;

	private RaftNodeConfig raftNodeConfig;

	private RaftLog raftLog;

	@Getter
	private RaftNodeState state;

	@Getter
	private RaftServerTransport raftServerTransport;

	public XRaftNode(RaftPeer raftPeer, RaftGroup raftGroup, RaftNodeConfig raftNodeConfig) {
		this.raftPeer = raftPeer;
		this.raftGroup = raftGroup;
		this.raftNodeConfig = raftNodeConfig;
		this.raftServerTransport = raftNodeConfig.getTransportType().newTransport(this);
		this.raftLog = raftNodeConfig.getRaftLogType().newRaftLog(this);
		this.state = new RaftNodeState(this);
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
	public RaftNodeConfig getRaftNodeConfig() {
		return raftNodeConfig;
	}

	@Override
	public RaftLog raftLog() {
		return raftLog;
	}

	@Override
	public synchronized void startup() {
		super.startup();
		// todo: init raft storage
		// todo: init raft log
		// todo: init state machine
		raftServerTransport.startup();
		raftServerTransport.addRaftPeers(state.getRaftConfiguration().getOtherRaftPeers());
		// todo: start role by config
		changeToFollower();
	}

	@Override
	public synchronized void shutdown() {
		super.shutdown();
	}

	@Override
	public synchronized VoteResponse requestVote(VoteRequest voteRequest) {
		Verify.verify(isStarted(), "RaftNode is not started yet.");
		// todo verify raft group

		VoteContext voteContext = new VoteContext(this, voteRequest);
		boolean voteGranted = voteContext.decideVote();
		// todo
		boolean shouldShutdown = false;

		return Responses.voteResponse(voteRequest.getRequestPeerId(), voteRequest.getReplyPeerId(),
				state.getCurrentTerm().get(), voteGranted, shouldShutdown);
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
		Long timeoutMills = raftNodeConfig.getElectionTimeoutMills();
		Long delayMills = raftNodeConfig.getElectionTimeoutDelayMills();
		return ThreadLocalRandom.current().nextLong(timeoutMills, timeoutMills + delayMills);
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
	public synchronized void changeToFollower(Long newTerm) {
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
