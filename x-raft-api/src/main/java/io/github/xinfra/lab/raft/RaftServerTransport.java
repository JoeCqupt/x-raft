package io.github.xinfra.lab.raft;

import java.util.Set;

public interface RaftServerTransport extends RaftServerProtocol, LifeCycle {

	void addRaftPeers(Set<RaftPeer> raftPeers);

}
