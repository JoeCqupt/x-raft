package io.github.xinfra.lab.raft.log;

import java.io.Closeable;
import java.util.List;

public interface RaftLog extends Closeable {

	long INVALID_LOG_TERM = -1L;

	long INVALID_LOG_INDEX = -1L;

	RaftMetadata loadMetadata();

	void persistMetadata(RaftMetadata raftMetadata);

	void append(LogEntry logEntry);

	/**
	 * @return the {@link TermIndex} of the last log entry.
	 */
	TermIndex getLastEntryTermIndex();

	/**
	 * Get log entry at the specified index.
	 * @param index the log index
	 * @return the log entry at the specified index, or null if not found
	 */
	LogEntry getEntry(Long index);

	/**
	 * Get log entries from startIndex (inclusive) to endIndex (exclusive).
	 * @param startIndex the start index (inclusive)
	 * @param endIndex the end index (exclusive)
	 * @return list of log entries in the specified range
	 */
	List<LogEntry> getEntries(Long startIndex, Long endIndex);

	Long getNextIndex();

}
