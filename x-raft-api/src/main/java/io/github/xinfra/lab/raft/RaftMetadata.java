package io.github.xinfra.lab.raft;

import lombok.Data;

import java.io.Serializable;

@Data
public class RaftMetadata implements Serializable {

	private Long term;

	private String voteFor;

	public RaftMetadata(Long term, String voteFor) {
		this.term = term;
		this.voteFor = voteFor;
	}

}
