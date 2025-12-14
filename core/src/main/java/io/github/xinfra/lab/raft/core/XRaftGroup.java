package io.github.xinfra.lab.raft.core;

import io.github.xinfra.lab.raft.AbstractLifeCycle;
import io.github.xinfra.lab.raft.RaftGroup;
import io.github.xinfra.lab.raft.RaftGroupOptions;
import io.github.xinfra.lab.raft.RaftNode;

public class XRaftGroup extends AbstractLifeCycle implements RaftGroup {

	private RaftGroupOptions raftGroupOptions;

	private String raftGroupId;

	private RaftNode raftNode;

	public XRaftGroup(RaftGroupOptions raftGroupOptions) {
		this.raftGroupOptions = raftGroupOptions;
		this.raftGroupId = raftGroupOptions.getRaftGroupId();
	}

	@Override
	public String getRaftGroupId() {
		return raftGroupId;
	}

	@Override
	public RaftNode getRaftNode() {
		return raftNode;
	}

	@Override
	public void startup() {
		super.startup();
		this.raftNode = new XRaftNode(raftGroupOptions.getRaftNodeOptions());
		this.raftNode.startup();
	}

	@Override
	public void shutdown() {
		super.shutdown();
		this.raftNode.shutdown();
	}

}
