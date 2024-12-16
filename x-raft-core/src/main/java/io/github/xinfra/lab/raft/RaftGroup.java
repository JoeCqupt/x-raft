package io.github.xinfra.lab.raft;

import java.util.Iterator;

public class RaftGroup {

    private String raftGroupId;

    private Iterator<RaftPeer> raftPeers;

    public RaftGroup(String raftGroupId, Iterator<RaftPeer> raftPeers) {
        this.raftGroupId = raftGroupId;
        this.raftPeers = raftPeers;
    }
}
