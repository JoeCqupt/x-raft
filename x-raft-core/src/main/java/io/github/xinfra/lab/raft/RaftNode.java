package io.github.xinfra.lab.raft;


public interface RaftNode extends LifeCycle {

    RaftPeer getPeer();

    RaftGroup getGroup();
}
