package io.github.xinfra.lab.raft.transport;

import io.github.xinfra.lab.raft.RaftNode;

public enum LocalTransportType implements TransportType {

	local;

	@Override
	public RaftServerTransport newTransport(RaftNode raftNode) {
		return new LocalRaftServerTransport(raftNode);
	}

}
