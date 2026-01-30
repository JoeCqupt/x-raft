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
    private RaftNode raftNode;
	List<LogEntry> logEntries = new ArrayList<>();

	// todo: why use fair use true?
	private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);

	private AtomicReference<RaftMetadata> raftMetadataReference = new AtomicReference<>(RaftMetadata.getDefault());

	public MemoryRaftLog(RaftNode raftNode) {
		this.raftNode = raftNode;
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
				return new TermIndex(RaftLog.INVALID_LOG_TERM, RaftLog.INVALID_LOG_INDEX);
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
            // notify
            raftNode.notifyLogAppended();
		}
		finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@Override
	public Long getNextIndex() {
		readWriteLock.readLock().lock();
		try {
			TermIndex lastEntryTermIndex = getLastEntryTermIndex();
			return lastEntryTermIndex.getIndex() + 1;
		}
		finally {
			readWriteLock.readLock().unlock();
		}
	}

	@Override
	public LogEntry getEntry(Long index) {
		readWriteLock.readLock().lock();
		try {
			if (index < 0 || index >= logEntries.size()) {
				return null;
			}
			return logEntries.get(index.intValue());
		}
		finally {
			readWriteLock.readLock().unlock();
		}
	}

	@Override
	public List<LogEntry> getEntries(Long startIndex, Long endIndex) {
		readWriteLock.readLock().lock();
		try {
			if (startIndex < 0 || startIndex >= logEntries.size()) {
				return new ArrayList<>();
			}
			int end = Math.min(endIndex.intValue(), logEntries.size());
			return new ArrayList<>(logEntries.subList(startIndex.intValue(), end));
		}
		finally {
			readWriteLock.readLock().unlock();
		}
	}

	@Override
	public void truncateAfter(Long afterIndex) {
		readWriteLock.writeLock().lock();
		try {
			if (afterIndex < 0) {
				// 截断所有日志
				logEntries.clear();
			}
			else if (afterIndex < logEntries.size() - 1) {
				// 删除 afterIndex 之后的所有日志
				// afterIndex + 1 是要保留的最后一个索引的下一个位置
				logEntries.subList(afterIndex.intValue() + 1, logEntries.size()).clear();
			}
			// 如果 afterIndex >= logEntries.size() - 1，不需要做任何操作
		}
		finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@Override
	public void close() throws IOException {
		// do nothing
	}

}
