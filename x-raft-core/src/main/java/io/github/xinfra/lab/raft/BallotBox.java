package io.github.xinfra.lab.raft;

import java.util.Set;

public class BallotBox {

	private final RaftConfiguration raftConfiguration;

	Set<RaftPeer> votedPeers;

	public BallotBox(RaftConfiguration raftConfiguration) {
		this.raftConfiguration = raftConfiguration;
	}

	public void grantVote(RaftPeer peer) {

		votedPeers.add(peer);
	}

	public boolean isMajorityGranted() {
		// todo
		return false;
	}

}
