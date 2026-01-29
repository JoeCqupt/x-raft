package io.github.xinfra.lab.raft.core.state;

import io.github.xinfra.lab.raft.core.XRaftNode;

public class LearnerState {

	private XRaftNode xRaftNode;

	private volatile boolean running;

	public LearnerState(XRaftNode xRaftNode) {
		this.xRaftNode = xRaftNode;
	}

	public void startup() {
		if (running) {
			return;
		}
		running = true;
	}

	public void shutdown() {
		if (!running) {
			return;
		}
		running = false;
	}

}
