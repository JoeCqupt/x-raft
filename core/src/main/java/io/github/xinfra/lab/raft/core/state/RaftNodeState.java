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

	@GuardByLock
	@Getter
    @Setter
	private  Long currentTerm = 0L;

	@GuardByLock
	@Getter
	@Setter
	private volatile String votedFor ;

	@GuardByLock
	@Setter
	@Getter
	private volatile String leaderId;

	@GuardByLock
	@Getter
	private volatile RaftRole role;

	private final XRaftNode xRaftNode;

	@GuardByLock
	private FollowerState followerState;

	@GuardByLock
	private CandidateState candidateState;

	@GuardByLock
	private LeaderState leaderState;

	@GuardByLock
	@Getter
	private RaftLog raftLog;

	@GuardByLock
	@Getter
	private RaftConfigurationState configState;

	@GuardByLock
	@Getter
	private volatile Long lastLeaderRpcTimeMills = System.currentTimeMillis();


	public RaftNodeState(XRaftNode xRaftNode, RaftLog raftLog, RaftConfigurationState configState) {
		this.xRaftNode = xRaftNode;
		this.raftLog = raftLog;
		this.configState = configState;
		this.followerState = new FollowerState(xRaftNode);
		this.candidateState = new CandidateState(xRaftNode);
		this.leaderState = new LeaderState(xRaftNode);
	}

	public void changeToFollower() {
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
	public  void changeToFollower(Long newTerm) {
        log.info("node:{} change to follower, new term:{}", xRaftNode.getRaftPeer(), newTerm);
		// todo
	}

	public void changeToCandidate()  {
			role = RaftRole.CANDIDATE;
			followerState.shutdown();
			candidateState.startup();
			log.info("node:{} change to candidate", xRaftNode.getRaftPeer());
	}

	public  void changeToLeader()  {
			role = RaftRole.LEADER;
			candidateState.shutdown();
			leaderState.startup();
			log.info("node:{} change to leader", xRaftNode.getRaftPeer());
	}

	public void persistMetadata() {
		raftLog.persistMetadata(new RaftMetadata(currentTerm, votedFor));
	}

	public void updateConfiguration() {
		configState.updateConfiguration();
	}

	public void resetLeaderId(String peerId) {
		// todo implement
	}
}
