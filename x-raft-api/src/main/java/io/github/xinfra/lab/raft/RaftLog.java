package io.github.xinfra.lab.raft;

import java.io.Closeable;

public interface RaftLog extends Closeable {

	/** The least valid log index, i.e. the index used when writing to an empty log. */
	long LEAST_VALID_LOG_INDEX = 0L;

	long INVALID_LOG_INDEX = LEAST_VALID_LOG_INDEX - 1;

	void persistMetadata(RaftMetadata raftMetadata);

	RaftMetadata loadMetadata();

	/**
	 * @return the {@link TermIndex} of the last log entry.
	 */
	TermIndex getLastEntryTermIndex();

	void append(LogEntry logEntry);

	Long getNextIndex();

}
