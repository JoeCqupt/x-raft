package io.github.xinfra.lab.raft;

public interface RaftLog {

    /** @return the {@link TermIndex} of the last log entry. */
    TermIndex getLastEntryTermIndex();
}
