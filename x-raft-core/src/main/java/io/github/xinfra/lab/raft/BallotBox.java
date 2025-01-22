package io.github.xinfra.lab.raft;

import java.util.List;
import java.util.Set;

public class BallotBox {

	private RaftNodeState state;

	List<RaftPeer> votedPeers;

	public BallotBox(RaftNodeState state) {
		this.state = state;
	}

	public void grantVote(RaftPeer peer) {

		votedPeers.add(peer);
	}

	public boolean isMajorityGranted() {
		// todo
		return false;
	}

}
