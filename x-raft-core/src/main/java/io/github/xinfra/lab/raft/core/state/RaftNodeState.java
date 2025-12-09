package io.github.xinfra.lab.raft.core.state;

import io.github.xinfra.lab.raft.RaftRole;
import io.github.xinfra.lab.raft.core.XRaftNode;
import io.github.xinfra.lab.raft.log.RaftMetadata;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class RaftNodeState {

	@Getter
	private final AtomicLong currentTerm = new AtomicLong();

	@Getter
	@Setter
	private volatile AtomicReference<String> votedFor = new AtomicReference<>(null);

	@Setter
	@Getter
	private volatile AtomicReference<String> leaderId = new AtomicReference<>(null);

	@Getter
	private volatile RaftRole role;

	private final XRaftNode xRaftNode;

	private final RaftConfigurationState raftConfigurationState;

	private  FollowerState followerState;

	private final AtomicReference<CandidateState> candidateState = new AtomicReference<>();

	private final AtomicReference<LeaderState> leaderState = new AtomicReference<>();

	public RaftNodeState(XRaftNode xRaftNode) {
		this.xRaftNode = xRaftNode;
		RaftConfiguration initialConfiguration = new RaftConfiguration(xRaftNode.self(), null,
				new PeerConfiguration(xRaftNode.getRaftGroup().getPeers()));
		this.raftConfigurationState = new RaftConfigurationState(initialConfiguration);
		this.followerState = new FollowerState(xRaftNode);
	}

	public synchronized void changeToFollower() {
		if (role == RaftRole.CANDIDATE) {
			shutdownCandidateState();
		}
		if (role == RaftRole.LEADER) {
			shutdownLeaderState();
		}
		if (role == RaftRole.LEARNER) {
			// todo
		}
		role = RaftRole.FOLLOWER;
		followerState.startup();
	}

	/**
	 * new term discovered, change to follower
	 * @param newTerm
	 */
	public synchronized void changeToFollower(Long newTerm) {
		// todo
	}

	public synchronized void changeToCandidate() {
		followerState.shutdown();
		startCandidateState();
	}

	public synchronized void changeToLeader() {
		shutdownCandidateState();
		startLeaderState();
	}

	private void shutdownFollowerState() {

	}

	private void startCandidateState() {
		role = RaftRole.CANDIDATE;
		candidateState.updateAndGet(current -> current == null ? new CandidateState(xRaftNode) : current).start();
	}

	private void shutdownCandidateState() {
		CandidateState candidate = candidateState.getAndSet(null);
		if (candidate != null) {
			candidate.shutdown();
			candidate.interrupt();
		}
	}

	private void startLeaderState() {
		role = RaftRole.LEADER;
		leaderState.updateAndGet(current -> current == null ? new LeaderState(xRaftNode) : current).start();
	}

	private void shutdownLeaderState() {
		LeaderState leader = leaderState.getAndSet(null);
		if (leader != null) {
			leader.shutdown();
			leader.interrupt();
		}
	}

	public RaftConfiguration getRaftConfiguration() {
		return raftConfigurationState.getCurrentConfiguration();
	}

	public void persistMetadata() {
		xRaftNode.raftLog().persistMetadata(new RaftMetadata(currentTerm.get(), votedFor.get()));
	}

}
