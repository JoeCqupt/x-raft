package io.github.xinfra.lab.raft.base;

import io.github.xinfra.lab.raft.String;
import io.github.xinfra.lab.raft.RaftNode;
import io.github.xinfra.lab.raft.RaftNodeOptions;
import io.github.xinfra.lab.raft.RaftPeerId;
import io.github.xinfra.lab.raft.core.XRaftNode;
import io.github.xinfra.lab.raft.core.log.MemoryRaftLogType;

import java.util.ArrayList;
import java.util.List;

import static io.github.xinfra.lab.raft.transport.LocalTransportType.local;

/**
 * for unit test
 */
public class TestRaftNode extends XRaftNode {

	private List<RaftNode> raftPeerNodes = new ArrayList<>();

	public TestRaftNode(RaftPeerId raftPeerId, String raftGroup) {
		super(raftPeerId, raftGroup, raftNodeConfig());
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
