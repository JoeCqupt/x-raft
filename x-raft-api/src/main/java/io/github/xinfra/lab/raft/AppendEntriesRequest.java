package io.github.xinfra.lab.raft;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class AppendEntriesRequest extends TransportInfo implements Serializable {

	private Long term;

	private Long leaderId;

	private Long prevLogIndex;

	private Long prevLogTerm;

	private List<LogEntry> entries;

	private Long leaderCommit;

}
