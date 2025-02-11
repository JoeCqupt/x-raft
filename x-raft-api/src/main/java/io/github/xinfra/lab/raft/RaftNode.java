package io.github.xinfra.lab.raft;

public interface RaftNode extends LifeCycle, RaftServerProtocol, RaftClientProtocol, AdminProtocol {

	RaftPeer self();

	RaftGroup getRaftGroup();

	RaftNodeConfig getRaftNodeConfig();

	RaftLog raftLog();

}
