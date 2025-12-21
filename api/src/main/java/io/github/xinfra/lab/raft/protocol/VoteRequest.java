package io.github.xinfra.lab.raft.protocol;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Data
public class VoteRequest extends RequestMessage implements Serializable {


	private boolean preVote;

	private Long term;

	private String candidateId;

	private Long lastLogIndex;

	private Long lastLogTerm;

}
