package io.github.xinfra.lab.raft;

public class LeaderState extends Thread {

	private final XRaftNode xRaftNode;

	public LeaderState(XRaftNode xRaftNode) {
		this.xRaftNode = xRaftNode;
	}

	@Override
	public void run() {
		// todo
	}

}
