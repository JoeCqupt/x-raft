package io.github.xinfra.lab.raft;

import java.io.Closeable;

public interface RaftLog extends Closeable {

	long INVALID_LOG_INDEX = -1;

	long INVALID_LOG_TERM = -1;

	void persistMetadata(RaftMetadata raftMetadata);

	RaftMetadata loadMetadata();

	/**
	 * @return the {@link TermIndex} of the last log entry.
	 */
	TermIndex getLastEntryTermIndex();

	void append(LogEntry logEntry);

	Long getNextIndex();

}
