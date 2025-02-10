package io.github.xinfra.lab.raft;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Setter
@Getter
public class AppendEntriesRequest extends TransportInfo implements Serializable {

	private Long term;

	private Long leaderId;

	private Long prevLogIndex;

	private Long prevLogTerm;

	private List<LogEntry> entries;

	private Long leaderCommit;

}
