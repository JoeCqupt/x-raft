package io.github.xinfra.lab.raft.core.state;

import io.github.xinfra.lab.raft.core.XRaftNode;

public class LearnerState {
    private XRaftNode xRaftNode;

    public LearnerState(XRaftNode xRaftNode) {
        this.xRaftNode = xRaftNode;
    }

    public void startup() {
    }
}
