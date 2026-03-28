package io.github.xinfra.lab.raft.core.log;

import io.github.xinfra.lab.raft.log.RaftLog;
import io.github.xinfra.lab.raft.log.RaftLogType;

public enum MemoryRaftLogType implements RaftLogType {

	memory;

	@Override
	public RaftLog newRaftLog() {
		return new MemoryRaftLog();
	}

}
