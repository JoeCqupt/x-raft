package io.github.xinfra.lab.raft;

public interface RaftLogType {
    RaftLog newRaftLog(RaftNode raftNode);
}
