package io.github.xinfra.lab.raft.transport;

import io.github.xinfra.lab.raft.LifeCycle;
import io.github.xinfra.lab.raft.RaftPeer;
import io.github.xinfra.lab.raft.protocol.RaftServerProtocol;

import java.util.Set;

public interface RaftServerTransport extends RaftServerProtocol, LifeCycle {

	void addRaftPeers(Set<RaftPeer> raftPeers);

}
