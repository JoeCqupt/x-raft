package io.github.xinfra.lab.raft.transport;

import io.github.xinfra.lab.raft.RaftNode;
import io.github.xinfra.lab.raft.RaftServerTransport;

public class RaftServerTransportFactory {

	public static RaftServerTransport create(TransportType transportType, RaftNode raftNode) {
		switch (transportType) {
			case xremoting:
				return new XRaftServerTransport(raftNode);
		}
		throw new IllegalArgumentException("xRaft unsupported transport type: " + transportType);
	}

}
