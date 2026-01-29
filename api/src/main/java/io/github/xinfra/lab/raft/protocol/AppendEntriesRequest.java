package io.github.xinfra.lab.raft.protocol;

import io.github.xinfra.lab.raft.log.LogEntry;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class AppendEntriesRequest extends RaftGroupAware {

	private Long term;

	private String leaderId;

	private Long prevLogIndex;

	private Long prevLogTerm;

	private List<LogEntry> entries;

	private Long leaderCommit;

}
