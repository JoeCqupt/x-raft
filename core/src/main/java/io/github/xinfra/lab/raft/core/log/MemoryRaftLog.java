package io.github.xinfra.lab.raft.core.log;

import io.github.xinfra.lab.raft.RaftNode;
import io.github.xinfra.lab.raft.log.LogEntry;
import io.github.xinfra.lab.raft.log.RaftLog;
import io.github.xinfra.lab.raft.log.RaftMetadata;
import io.github.xinfra.lab.raft.log.TermIndex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MemoryRaftLog implements RaftLog {

	List<LogEntry> logEntries = new ArrayList<>();

	// todo: why use fair use true?
	private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);

	private AtomicReference<RaftMetadata> raftMetadataReference = new AtomicReference<>(RaftMetadata.getDefault());

	public MemoryRaftLog(RaftNode raftNode) {
		// todo
	}

	@Override
	public void persistMetadata(RaftMetadata raftMetadata) {
		raftMetadataReference.set(raftMetadata);
	}

	@Override
	public RaftMetadata loadMetadata() {
		return raftMetadataReference.get();
	}

	@Override
	public TermIndex getLastEntryTermIndex() {
		readWriteLock.readLock().lock();
		try {
			if (logEntries.size() > 0) {
				LogEntry last = logEntries.get(logEntries.size() - 1);
				return new TermIndex(last.term(), last.index());
			}
			else {
				return new TermIndex(INVALID_LOG_TERM, INVALID_LOG_INDEX);
			}
		}
		finally {
			readWriteLock.readLock().unlock();
		}
	}

	@Override
	public void append(LogEntry logEntry) {
		readWriteLock.writeLock().lock();
		try {
			logEntries.add(logEntry);
		}
		finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@Override
	public Long getNextIndex() {
		TermIndex lastEntryTermIndex = getLastEntryTermIndex();
		return lastEntryTermIndex.getIndex() + 1;
	}

	@Override
	public void close() throws IOException {
		// do nothing
	}

}
