package io.github.xinfra.lab.raft.protocol;

import io.github.xinfra.lab.raft.log.LogEntry;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AppendEntriesRequest extends RaftGroupAware {

	private Long term;

	private String leaderId;

	private Long prevLogIndex;

	private Long prevLogTerm;

	private List<LogEntry> entries;

	private Long leaderCommit;

}
