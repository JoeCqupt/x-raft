package io.github.xinfra.lab.raft.transport;

import io.github.xinfra.lab.raft.RaftNode;
import io.github.xinfra.lab.raft.RaftServerTransport;
import io.github.xinfra.lab.raft.TransportType;

import static io.github.xinfra.lab.raft.transport.SupportedTransportType.xremoting;

public class RaftServerTransportFactory {

	public static RaftServerTransport create(SupportedTransportType transportType, RaftNode raftNode) {
		switch (transportType) {
			case xremoting:
				return new XRaftServerTransport(raftNode);
		}
		throw new IllegalArgumentException("xRaft unsupported transport type: " + transportType);
	}

}
