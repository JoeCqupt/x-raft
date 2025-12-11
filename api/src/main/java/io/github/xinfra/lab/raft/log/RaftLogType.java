package io.github.xinfra.lab.raft.log;

import io.github.xinfra.lab.raft.RaftNode;

public interface RaftLogType {

	RaftLog newRaftLog(RaftNode raftNode);

}
