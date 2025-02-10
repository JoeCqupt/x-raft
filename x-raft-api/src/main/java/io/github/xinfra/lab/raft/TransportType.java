package io.github.xinfra.lab.raft;

public interface TransportType {

	RaftServerTransport newTransport(RaftNode raftNode);

}
