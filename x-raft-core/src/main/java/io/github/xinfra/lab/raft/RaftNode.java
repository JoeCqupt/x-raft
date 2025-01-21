package io.github.xinfra.lab.raft;


public interface RaftNode extends
        LifeCycle,
        RaftServerProtocol,
        RaftClientProtocol,
        AdminProtocol {

    RaftPeer getPeer();

    RaftGroup getGroup();

    RaftServerProtocol getRaftServer(RaftPeer raftPeer);
}
