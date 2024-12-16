package io.github.xinfra.lab.raft;

import java.util.Comparator;

public interface TermIndex extends Comparable<TermIndex> {
    /** @return the term. */
    long getTerm();

    /** @return the index. */
    long getIndex();

    @Override
    default int compareTo(TermIndex that) {
        return Comparator.comparingLong(TermIndex::getTerm)
                .thenComparingLong(TermIndex::getIndex)
                .compare(this, that);
    }
}
