package io.github.xinfra.lab.raft.log;

import lombok.Getter;
import lombok.ToString;

import java.util.Comparator;

@Getter
@ToString
public class TermIndex implements Comparable<TermIndex> {

	private Long term;

	private Long index;

	public TermIndex(Long term, Long index) {
		this.term = term;
		this.index = index;
	}

	@Override
	public int compareTo(TermIndex that) {
		return Comparator.comparingLong(TermIndex::getTerm).thenComparingLong(TermIndex::getIndex).compare(this, that);
	}

}
