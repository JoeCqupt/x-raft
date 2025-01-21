package io.github.xinfra.lab.raft;

import lombok.Data;

import java.util.Set;


@Data
public class RaftGroup {

    private String raftGroupId;

    private Set<RaftPeer> raftPeers;

    public RaftGroup(String raftGroupId, Set<RaftPeer> raftPeers) {
        this.raftGroupId = raftGroupId;
        this.raftPeers = raftPeers;
    }

}
