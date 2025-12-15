package io.github.xinfra.lab.raft.core.conf;

import io.github.xinfra.lab.raft.RaftPeerId;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

@Data
public class Configuration implements Serializable {

    /**
     * All peers in the raft group. LEADER, CANDIDATE, FOLLOWER and Listener
     */
    private final List<RaftPeerId> peers;

    /**
     * Learners are non-voting members.
     */
    private final List<RaftPeerId> learners;

    public Configuration(List<RaftPeerId> peers, List<RaftPeerId> learners) {
        this.peers = peers;
        this.learners = learners;
    }

}
