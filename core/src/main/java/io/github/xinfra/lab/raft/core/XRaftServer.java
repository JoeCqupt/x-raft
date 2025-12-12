package io.github.xinfra.lab.raft.core;

import io.github.xinfra.lab.raft.AbstractLifeCycle;
import io.github.xinfra.lab.raft.RaftGroup;
import io.github.xinfra.lab.raft.RaftGroupOptions;
import io.github.xinfra.lab.raft.RaftServer;

import java.util.List;

public class XRaftServer extends AbstractLifeCycle implements RaftServer  {

    public XRaftServer() {
    }

    @Override
    public void startRaftGroup(RaftGroupOptions raftGroupOptions) {
        // todo @joecqupt
    }

    @Override
    public List<RaftGroup> getRaftGroups() {
        // todo @joecqupt
        return null;
    }
}
