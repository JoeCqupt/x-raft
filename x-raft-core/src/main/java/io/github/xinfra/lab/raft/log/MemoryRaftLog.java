package io.github.xinfra.lab.raft.log;

import io.github.xinfra.lab.raft.LogEntry;
import io.github.xinfra.lab.raft.RaftLog;
import io.github.xinfra.lab.raft.RaftMetadata;
import io.github.xinfra.lab.raft.TermIndex;

import java.util.ArrayList;
import java.util.List;

public class MemoryRaftLog implements RaftLog {

    List<LogEntry> logEntries = new ArrayList<>();

    @Override
    public TermIndex getLastEntryTermIndex() {
        // todo
        return null;
    }

    @Override
    public void persistMetadata(RaftMetadata raftMetadata) {
        // todo
    }

    @Override
    public void append(LogEntry logEntry) {
        // todo
    }

    @Override
    public Long getNextIndex() {
        // todo
        return (long) (logEntries.size() + 1);
    }
}
