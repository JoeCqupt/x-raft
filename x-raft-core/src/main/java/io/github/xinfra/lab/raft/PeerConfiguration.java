package io.github.xinfra.lab.raft;

import lombok.Getter;

import java.util.Map;

public class PeerConfiguration {

    /**
     * Peers are voting members such as LEADER, CANDIDATE and FOLLOWER
     */
    @Getter
    private final Map<RaftPeer, RaftPeer> peers;
    /**
     * Listeners are non-voting members.
     */
    @Getter
    private final Map<RaftPeer, RaftPeer> listeners;

    public PeerConfiguration(Map<RaftPeer, RaftPeer> peers, Map<RaftPeer, RaftPeer> listeners) {
        this.peers = peers;
        this.listeners = listeners;
    }

}
