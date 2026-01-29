package io.github.xinfra.lab.raft.protocol;

import lombok.Data;

@Data
public class VoteRequest extends RaftGroupAware {

	private boolean preVote;

	private Long term;

	private String candidateId;

	private Long lastLogIndex;

	private Long lastLogTerm;

}
