package io.github.xinfra.lab.raft;

import java.util.List;

public class LocalXRaftNode extends XRaftNode {

	public LocalXRaftNode(RaftPeer raftPeer, RaftGroup raftGroup, RaftNodeConfig raftNodeConfig) {
		super(raftPeer, raftGroup, raftNodeConfig);
	}

	public List<RaftNode> otherRaftNodes() {
		// todo
		return null;
	}

}
