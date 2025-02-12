package io.github.xinfra.lab.raft.transport;

import io.github.xinfra.lab.raft.RaftNode;

public interface TransportType {

	RaftServerTransport newTransport(RaftNode raftNode);

}
