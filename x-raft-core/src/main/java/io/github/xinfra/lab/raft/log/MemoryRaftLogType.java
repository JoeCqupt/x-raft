package io.github.xinfra.lab.raft.log;

import io.github.xinfra.lab.raft.RaftLog;
import io.github.xinfra.lab.raft.RaftLogType;
import io.github.xinfra.lab.raft.RaftNode;

public enum MemoryRaftLogType implements RaftLogType {

	memory;

	@Override
	public RaftLog newRaftLog(RaftNode raftNode) {
		return new MemoryRaftLog(raftNode);
	}

}
