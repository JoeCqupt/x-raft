package io.github.xinfra.lab.raft.protocol;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class VoteRequest extends Message implements Serializable {

	private boolean preVote;

	private Long term;

	private String candidateId;

	private Long lastLogIndex;

	private Long lastLogTerm;

}
