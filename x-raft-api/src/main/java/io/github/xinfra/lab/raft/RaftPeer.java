package io.github.xinfra.lab.raft;

import lombok.Data;

import java.net.InetSocketAddress;
import java.util.Objects;

@Data
public class RaftPeer {

    private String raftPeerId;

    private InetSocketAddress address;

    private RaftRole role = RaftRole.FOLLOWER;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RaftPeer)) return false;
        RaftPeer raftPeer = (RaftPeer) o;
        return Objects.equals(raftPeerId, raftPeer.raftPeerId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(raftPeerId);
    }
}
