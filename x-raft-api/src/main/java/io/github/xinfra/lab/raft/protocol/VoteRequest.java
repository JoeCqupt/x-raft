package io.github.xinfra.lab.raft.protocol;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class VoteRequest extends BaseInfo implements Serializable {

	private Long term;

	private String candidateId;

	private Long lastLogIndex;

	private Long lastLogTerm;

	private boolean preVote;

}
