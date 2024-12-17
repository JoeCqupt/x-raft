package io.github.xinfra.lab.raft;

import lombok.Getter;

import java.util.Set;


public class RaftGroup {

    private String raftGroupId;

    @Getter
    private Set<RaftPeer> raftPeers;

    public RaftGroup(String raftGroupId, Set<RaftPeer> raftPeers) {
        this.raftGroupId = raftGroupId;
        this.raftPeers = raftPeers;
    }

}
