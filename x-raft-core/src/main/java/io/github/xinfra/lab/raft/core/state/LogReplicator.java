package io.github.xinfra.lab.raft.core.state;

import io.github.xinfra.lab.raft.RaftPeer;
import io.github.xinfra.lab.raft.RaftRole;
import io.github.xinfra.lab.raft.core.XRaftNode;
import io.github.xinfra.lab.raft.protocol.AppendEntriesRequest;
import lombok.Data;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Data
public class LogReplicator extends Thread {

	private volatile boolean running = true;

	private final RaftPeer raftPeer;

	private final XRaftNode xRaftNode;

	private Long nextIndex;

	private Long matchIndex = -1L;

	// todo : await and notify
	private final Lock awaitLock = new ReentrantLock();

	private Long lastAppendSendTime;

	public LogReplicator(RaftPeer raftPeer, XRaftNode xRaftNode) {
		this.raftPeer = raftPeer;
		this.xRaftNode = xRaftNode;
		this.nextIndex = xRaftNode.raftLog().getNextIndex();
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

		AppendEntriesRequest request = new AppendEntriesRequest();
		request.setLeaderId(xRaftNode.getState().getLeaderId().get());

	}

	private boolean shouldAppend() {
		return hasEntries() || heartbeatLeftTimeMills() <= 0;
	}

	private Long heartbeatLeftTimeMills() {
		if (lastAppendSendTime == 0) {
			// run first time
			return 0L;
		}
		// todo: to calculate it
		// todo: why
		Long electionTimeoutMills = xRaftNode.getRaftNodeConfig().getElectionTimeoutMills();
		Long noHeartbeatTimeMills = System.currentTimeMillis() - lastAppendSendTime;
		return (electionTimeoutMills / 3) - noHeartbeatTimeMills;
	}

	private boolean hasEntries() {
		return nextIndex < xRaftNode.raftLog().getNextIndex();
	}

	private boolean shouldRun() {
		return running && xRaftNode.getState().getRole() == RaftRole.LEADER;
	}

	public void shutdown() {
		running = false;
	}

}
