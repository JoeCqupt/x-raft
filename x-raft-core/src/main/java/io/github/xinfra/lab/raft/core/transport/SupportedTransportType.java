package io.github.xinfra.lab.raft.core.transport;

import io.github.xinfra.lab.raft.RaftNode;
import io.github.xinfra.lab.raft.transport.RaftServerTransport;
import io.github.xinfra.lab.raft.transport.TransportType;

public enum SupportedTransportType implements TransportType {

	xremoting, grpc,;

	@Override
	public RaftServerTransport newTransport(RaftNode raftNode) {
		return RaftServerTransportFactory.create(this, raftNode);
	}

}
