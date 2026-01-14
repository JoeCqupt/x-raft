package io.github.xinfra.lab.raft.log;

import java.io.Closeable;

public interface RaftLog extends Closeable {

    RaftMetadata loadMetadata();

    void persistMetadata(RaftMetadata raftMetadata);

    void append(LogEntry logEntry);

    /**
     * @return the {@link TermIndex} of the last log entry.
     */
    TermIndex getLastEntryTermIndex();


    default Long getNextIndex() {
        return getLastEntryTermIndex().getIndex() + 1;
    }
}
