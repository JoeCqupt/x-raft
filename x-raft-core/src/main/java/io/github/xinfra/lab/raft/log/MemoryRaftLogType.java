package io.github.xinfra.lab.raft.log;

import io.github.xinfra.lab.raft.RaftLog;
import io.github.xinfra.lab.raft.RaftLogType;
import io.github.xinfra.lab.raft.RaftNode;

public enum MemoryRaftLogType implements RaftLogType {
    mem;

    @Override
    public RaftLog newRaftLog(RaftNode raftNode) {
        return null;
    }
}
