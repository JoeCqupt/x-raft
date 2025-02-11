package io.github.xinfra.lab.raft;

import io.github.xinfra.lab.raft.log.MemoryRaftLogType;

import java.util.ArrayList;
import java.util.List;

import static io.github.xinfra.lab.raft.transport.LocalTransportType.local;

/**
 * for unit test
 */
public class LocalXRaftNode extends XRaftNode {

	private List<RaftNode> raftPeerNodes = new ArrayList<>();

	public LocalXRaftNode(RaftPeer raftPeer, RaftGroup raftGroup) {
		super(raftPeer, raftGroup, raftNodeConfig());
	}

	private static RaftNodeConfig raftNodeConfig() {
		RaftNodeConfig raftNodeConfig = new RaftNodeConfig();
		raftNodeConfig.setTransportType(local);
		raftNodeConfig.setRaftLogType(MemoryRaftLogType.memory);
		return raftNodeConfig;
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
