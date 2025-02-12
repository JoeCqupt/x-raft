package io.github.xinfra.lab.raft.core.transport;

import io.github.xinfra.lab.raft.RaftNode;
import io.github.xinfra.lab.raft.transport.RaftServerTransport;
import io.github.xinfra.lab.raft.transport.xremoting.XRaftServerTransport;

public class RaftServerTransportFactory {

	public static RaftServerTransport create(SupportedTransportType transportType, RaftNode raftNode) {
		switch (transportType) {
			case xremoting:
				return new XRaftServerTransport(raftNode);
		}
		throw new IllegalArgumentException("xRaft unsupported transport type: " + transportType);
	}

}
