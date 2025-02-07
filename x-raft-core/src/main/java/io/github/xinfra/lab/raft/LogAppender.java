package io.github.xinfra.lab.raft;

import lombok.Data;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Data
public class LogAppender extends Thread {

	private volatile boolean running = true;

	private final RaftPeer raftPeer;

	private final XRaftNode xRaftNode;

	private Long nextIndex;

	private Long matchIndex = -1L;

	// todo : await and notify
	private final Lock awaitLock = new ReentrantLock();

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

		return hasEntries() || heartbeatTimeoutMills() <= 0;
	}

	private long heartbeatTimeoutMills() {
		// todo : to calculate it
		return 0;
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
