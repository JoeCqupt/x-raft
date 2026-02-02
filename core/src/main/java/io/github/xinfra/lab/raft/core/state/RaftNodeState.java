package io.github.xinfra.lab.raft.core.state;

import io.github.xinfra.lab.raft.RaftRole;
import io.github.xinfra.lab.raft.core.XRaftNode;
import io.github.xinfra.lab.raft.core.annotation.GuardByLock;
import io.github.xinfra.lab.raft.core.conf.RaftConfigurationState;
import io.github.xinfra.lab.raft.log.RaftLog;
import io.github.xinfra.lab.raft.log.RaftMetadata;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
public class RaftNodeState {

	/**
	 * guard lock for state transition
	 */
	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	@Getter
	private final Lock writeLock = lock.writeLock();

	@Getter
	private final Lock readLock = lock.readLock();

	private final XRaftNode xRaftNode;

	@GuardByLock
	@Getter
	@Setter
	private Long currentTerm = 0L;

	@GuardByLock
	@Getter
	@Setter
	private volatile String votedFor;

	@GuardByLock
	@Setter
	@Getter
	private volatile String leaderId;

	@GuardByLock
	@Getter
	private volatile RaftRole role;

	@GuardByLock
	private FollowerState followerState;

	@GuardByLock
	private CandidateState candidateState;

	@GuardByLock
	private LeaderState leaderState;

	@GuardByLock
	private LearnerState learnerState;

	@GuardByLock
	@Getter
	private RaftLog raftLog;

	@Getter
	@Setter
	private volatile Long commitIndex = 0L;

	@GuardByLock
	@Getter
	private RaftConfigurationState configState;

	@GuardByLock
	@Getter
	@Setter
	private volatile Long lastLeaderRpcTimeMills = System.currentTimeMillis();

	public RaftNodeState(XRaftNode xRaftNode, RaftLog raftLog, RaftConfigurationState configState) {
		this.xRaftNode = xRaftNode;
		this.raftLog = raftLog;
		this.configState = configState;
		this.followerState = new FollowerState(xRaftNode);
		this.candidateState = new CandidateState(xRaftNode);
		this.leaderState = new LeaderState(xRaftNode);
		this.learnerState = new LearnerState(xRaftNode);
	}

	public void changeToFollower() {
		if (role == RaftRole.FOLLOWER) {
			return;
		}
		if (role == RaftRole.CANDIDATE) {
			candidateState.shutdown();
		}
		if (role == RaftRole.LEADER) {
			leaderState.shutdown();
		}
		if (role == RaftRole.LEARNER) {
			// todo
		}
		role = RaftRole.FOLLOWER;
		followerState.startup();
		log.info("node:{} change to follower", xRaftNode.getRaftPeer());
	}

	/**
	 * new term discovered, change to follower
	 * @param newTerm
	 */
	public void changeToFollower(Long newTerm) {
		log.info("node:{} change to follower, new term:{}", xRaftNode.getRaftPeer(), newTerm);
		this.currentTerm = newTerm;
		this.votedFor = null;
		// todo handle IOException
		persistMetadata();
		changeToFollower();
	}

	public void changeToCandidate() {
		if (role == RaftRole.CANDIDATE) {
			return;
		}
		role = RaftRole.CANDIDATE;
		followerState.shutdown();
		candidateState.startup();
		log.info("node:{} change to candidate", xRaftNode.getRaftPeer());
	}

	public void changeToLeader() {
		if (role == RaftRole.LEADER) {
			return;
		}
		role = RaftRole.LEADER;
		candidateState.shutdown();
		leaderState.startup();
		log.info("node:{} change to leader", xRaftNode.getRaftPeer());
	}

	public void changeToLearner() {
		if (role == RaftRole.LEARNER) {
			return;
		}
		role = RaftRole.LEARNER;
		learnerState.startup();
		log.info("node:{} change to learner", xRaftNode.getRaftPeer());
	}

	public void persistMetadata() {
		raftLog.persistMetadata(new RaftMetadata(currentTerm, votedFor));
	}

	public void updateConfiguration() {
		configState.updateConfiguration();
	}

	public void resetLeaderId(String peerId) {
		String oldLeaderId = this.leaderId;
		this.leaderId = peerId;

		if (!Objects.equals(oldLeaderId, peerId)) {
			log.info("Leader changed from {} to {}", oldLeaderId, peerId);
			// 通知状态机 Leader 变更
			if (xRaftNode.getStateMachine() != null) {
				xRaftNode.getStateMachine().onLeaderChange(peerId);
			}
		}
	}

	public void notifyLogAppended() {
		if (role == RaftRole.LEADER) {
			if (leaderState != null) {
				log.debug("node:{} notify log appended", xRaftNode.getRaftPeer());
				leaderState.notifyLogChanged();
			}
		}
	}

}
