package io.github.xinfra.lab.raft.base;

import io.github.xinfra.lab.raft.RaftNode;
import io.github.xinfra.lab.raft.RaftPeerId;
import io.github.xinfra.lab.raft.RaftRole;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class TestXRaftGroup {

	private String raftGroupId;

	List<TestRaftNode> raftNodes;

	List<RaftPeerId> raftPeerIds;

	private String raftGroup;

	private int nodeNums;

	private static int initPort = 5000;

	public TestXRaftGroup(String raftGroupId, int nodeNums) {
		this.raftGroupId = raftGroupId;
		this.nodeNums = nodeNums;
	}

	public void startup() {
		startupNodes();
	}

	private void startupNodes() {
		// init raftPeers
		raftPeerIds = new ArrayList<>();
		for (int i = 0; i < nodeNums; i++) {
			RaftPeerId raftPeerId = new RaftPeerId();
			raftPeerId.setPeerId("node" + i);
			raftPeerId.setAddress(new InetSocketAddress("localhost", 5000 + i));
			raftPeerIds.add(raftPeerId);
		}
		// init raftGroup
		raftGroup = new String(raftGroupId, raftPeerIds);
		// init raftNodes
		raftNodes = new ArrayList<>();
		for (RaftPeerId raftPeerId : raftPeerIds) {
			TestRaftNode testRaftNode = new TestRaftNode(raftPeerId, raftGroup);
			testRaftNode.addRaftPeerNode();
			raftNodes.add(testRaftNode);
		}
		// add raftPeerNode
		for (TestRaftNode raftNode : raftNodes) {
			for (RaftNode otherRaftNode : raftNodes) {
				if (raftNode != otherRaftNode) {
					raftNode.addRaftPeerNode(otherRaftNode);
				}
			}
		}

		// startup raftNodes
		for (TestRaftNode raftNode : raftNodes) {
			raftNode.startup();
		}
	}

	public RaftPeerId getLeaderPeer() {
		for (TestRaftNode raftNode : raftNodes) {
			if (raftNode.getState().getRole().equals(RaftRole.LEADER)) {
				return raftNode.raftPeerId();
			}
		}
		return null;
	}

}
