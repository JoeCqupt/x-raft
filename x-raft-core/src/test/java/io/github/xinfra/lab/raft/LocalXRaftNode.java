package io.github.xinfra.lab.raft;

import java.util.ArrayList;
import java.util.List;

/**
 * for unit test
 */
public class LocalXRaftNode extends XRaftNode {

	private List<RaftNode> raftPeerNodes = new ArrayList<>();

	public LocalXRaftNode(RaftPeer raftPeer, RaftGroup raftGroup) {
		super(raftPeer, raftGroup, new RaftNodeConfig());
	}

	/**
	 * get all raft peer nodes
	 * @return
	 */
	public List<RaftNode> raftPeerNodes() {
		return raftPeerNodes;
	}

	public void addRaftPeerNode(RaftNode raftNode) {
		raftPeerNodes.add(raftNode);
	}

}
