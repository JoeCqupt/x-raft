package io.github.xinfra.lab.raft.base;

import io.github.xinfra.lab.raft.String;
import io.github.xinfra.lab.raft.RaftNode;
import io.github.xinfra.lab.raft.RaftNodeOptions;
import io.github.xinfra.lab.raft.RaftPeer;
import io.github.xinfra.lab.raft.core.XRaftNode;
import io.github.xinfra.lab.raft.core.log.MemoryRaftLogType;

import java.util.ArrayList;
import java.util.List;

import static io.github.xinfra.lab.raft.transport.LocalTransportType.local;

/**
 * for unit test
 */
public class LocalXRaftNode extends XRaftNode {

	private List<RaftNode> raftPeerNodes = new ArrayList<>();

	public LocalXRaftNode(RaftPeer raftPeer, String raftGroup) {
		super(raftPeer, raftGroup, raftNodeConfig());
	}

	private static RaftNodeOptions raftNodeConfig() {
		RaftNodeOptions raftNodeOptions = new RaftNodeOptions();
		raftNodeOptions.setTransportType(local);
		raftNodeOptions.setRaftLogType(MemoryRaftLogType.memory);
		return raftNodeOptions;
	}

	/**
	 * get all raft peer nodes
	 * @return
	 */
	public List<RaftNode> raftPeerNodes() {
		return raftPeerNodes;
	}

	public void addRaftPeerNode(RaftNode... raftNodes) {
		for (RaftNode raftNode : raftNodes) {
			raftPeerNodes.add(raftNode);
		}
	}

}
