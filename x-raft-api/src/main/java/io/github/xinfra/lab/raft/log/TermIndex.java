package io.github.xinfra.lab.raft.log;

import lombok.Getter;

import java.util.Comparator;

@Getter
public class TermIndex implements Comparable<TermIndex> {

	private long term;

	private long index;

	public TermIndex(long term, long index) {
		this.term = term;
		this.index = index;
	}

	@Override
	public int compareTo(TermIndex that) {
		return Comparator.comparingLong(TermIndex::getTerm).thenComparingLong(TermIndex::getIndex).compare(this, that);
	}

}
