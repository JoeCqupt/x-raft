package io.github.xinfra.lab.raft;

import lombok.Data;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Data
public class LogAppender extends Thread {

	private volatile boolean running = true;

	private final RaftPeer raftPeer;

	private final XRaftNode xRaftNode;

	private long nextIndex;

	private long matchIndex = -1L;

	// todo : await and notify
	private final Lock awaitLock = new ReentrantLock();

	private long lastAppendSendTime;

	public LogAppender(RaftPeer raftPeer, XRaftNode xRaftNode) {
		this.raftPeer = raftPeer;
		this.xRaftNode = xRaftNode;
		this.nextIndex = xRaftNode.getState().getRaftLog().getNextIndex();
	}

	@Override
	public void run() {
		while (shouldRun()) {
			if (shouldAppend()) {
				sendAppendEntries();
			}
		}
	}

	private void sendAppendEntries() {
		// todo

	}

	private boolean shouldAppend() {
		return hasEntries() || heartbeatLeftTimeMills() <= 0;
	}

	private long heartbeatLeftTimeMills() {
		if (lastAppendSendTime == 0) {
			// run first time
			return 0;
		}
		// todo: to calculate it
		// todo: why
		long electionTimeoutMills = xRaftNode.getRaftNodeConfig().getElectionTimeoutMills();
		long noHeartbeatTimeMills = System.currentTimeMillis() - lastAppendSendTime;
		return (electionTimeoutMills / 3) - noHeartbeatTimeMills;
	}

	private boolean hasEntries() {
		return nextIndex < xRaftNode.getState().getRaftLog().getNextIndex();
	}

	private boolean shouldRun() {
		return running && xRaftNode.getState().getRole() == RaftRole.LEADER;
	}

	public void shutdown() {
		running = false;
	}

}
