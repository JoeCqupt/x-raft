package io.github.xinfra.lab.raft.core.state;

import io.github.xinfra.lab.raft.RaftRole;
import io.github.xinfra.lab.raft.core.XRaftNode;
import io.github.xinfra.lab.raft.log.RaftMetadata;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


@Slf4j
public class RaftNodeState {


	/**
	 * Guard lock for all state transitions.
	 */
	@Getter
	private final Lock lock = new ReentrantLock();

	@Getter
	private final AtomicLong currentTerm = new AtomicLong();

	@Getter
	@Setter
	// todo: 类型？
	private volatile AtomicReference<String> votedFor = new AtomicReference<>(null);

	@Setter
	@Getter
	private volatile AtomicReference<String> leaderId = new AtomicReference<>(null);

	@Getter
	private volatile RaftRole role;

	private final XRaftNode xRaftNode;

	private FollowerState followerState;

	private CandidateState candidateState;

	private LeaderState leaderState;



	public RaftNodeState(XRaftNode xRaftNode) {
		this.xRaftNode = xRaftNode;
		this.followerState = new FollowerState(xRaftNode);
		this.candidateState = new CandidateState(xRaftNode);
		this.leaderState = new LeaderState(xRaftNode);
	}

	public void changeToFollower() {
		try {
			lock.lockInterruptibly();
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
			log.info("node:{} change to follower", xRaftNode.raftPeerId());
		} catch (InterruptedException e) {
            Thread.currentThread().interrupt();
			log.error("interrupted when change to follower", e);
        } finally {
			lock.unlock();
		}
	}

	/**
	 * new term discovered, change to follower
	 * @param newTerm
	 */
	public  void changeToFollower(Long newTerm) {
		// todo
	}

	public boolean changeToCandidate() throws InterruptedException {
		try {
			lock.lockInterruptibly();
			if (role != RaftRole.FOLLOWER) {
				return false;
			}
			role = RaftRole.CANDIDATE;
			followerState.shutdown();
			candidateState.startup();
			log.info("node:{} change to candidate", xRaftNode.raftPeerId());
			return true;
		}  finally {
			lock.unlock();
		}
	}

	public  boolean changeToLeader() throws InterruptedException {
		try {
			lock.lockInterruptibly();
			if (role != RaftRole.CANDIDATE) {
				return false;
			}
			role = RaftRole.LEADER;
			candidateState.shutdown();
			leaderState.startup();
			log.info("node:{} change to leader", xRaftNode.raftPeerId());
			return true;
		} finally {
			lock.unlock();
		}

	}

	public void persistMetadata() {
		xRaftNode.raftLog().persistMetadata(new RaftMetadata(currentTerm.get(), votedFor.get()));
	}

	public void startElection(boolean preVote) throws InterruptedException {
		try {
			lock.lockInterruptibly();
			// todo: notify state machine
			leaderId.getAndSet(null);
			long electionTerm;
			if (preVote) {
				electionTerm = xRaftNode.getState().getCurrentTerm().get();
			} else {
				electionTerm = xRaftNode.getState().getCurrentTerm().incrementAndGet();
				xRaftNode.getState().getVotedFor().getAndSet(xRaftNode.raftPeerId().getPeerId());
				xRaftNode.getState().persistMetadata();
			}
		} finally {
			lock.unlock();
		}
	}
}
