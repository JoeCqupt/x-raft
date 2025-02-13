package io.github.xinfra.lab.raft.log;

import java.io.Closeable;

public interface RaftLog extends Closeable {

	Long INVALID_LOG_INDEX = -1l;

	Long INVALID_LOG_TERM = -1l;

	void persistMetadata(RaftMetadata raftMetadata);

	RaftMetadata loadMetadata();

	/**
	 * @return the {@link TermIndex} of the last log entry.
	 */
	TermIndex getLastEntryTermIndex();

	void append(LogEntry logEntry);

	Long getNextIndex();

}
