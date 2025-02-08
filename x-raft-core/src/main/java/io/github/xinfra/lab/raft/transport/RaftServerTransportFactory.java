package io.github.xinfra.lab.raft.transport;

import io.github.xinfra.lab.raft.RaftNode;
import io.github.xinfra.lab.raft.RaftServerTransport;
import io.github.xinfra.lab.raft.TransportType;

public class RaftServerTransportFactory {

	public static RaftServerTransport create(TransportType transportType, RaftNode raftNode) {
		switch (transportType) {
			case xremoting:
				return new XRaftServerTransport(raftNode);
		}
		throw new IllegalArgumentException("xRaft unsupported transport type: " + transportType);
	}

}
