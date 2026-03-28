package io.github.xinfra.lab.raft.core;

import io.github.xinfra.lab.raft.AbstractLifeCycle;

public class XRaftGroup extends AbstractLifeCycle {

	private final RaftGroupOptions raftGroupOptions;

	private final String raftGroupId;

	private XRaftNode raftNode;

	public XRaftGroup(RaftGroupOptions raftGroupOptions) {
		this.raftGroupOptions = raftGroupOptions;
		this.raftGroupId = raftGroupOptions.getRaftGroupId();
	}

	public String getRaftGroupId() {
		return raftGroupId;
	}

	public XRaftNode getRaftNode() {
		return raftNode;
	}

	@Override
	public void startup() {
		super.startup();
		this.raftNode = new XRaftNode(raftGroupId, raftGroupOptions.getRaftNodeOptions());
		this.raftNode.startup();
	}

	@Override
	public void shutdown() {
		super.shutdown();
		this.raftNode.shutdown();
	}

}
