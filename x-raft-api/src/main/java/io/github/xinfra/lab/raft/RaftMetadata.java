package io.github.xinfra.lab.raft;

import lombok.Data;

import java.io.Serializable;

@Data
public class RaftMetadata implements Serializable {

	private static final RaftMetadata DEFAULT = new RaftMetadata(0L, null);

	private Long term;

	private String voteFor;

	public RaftMetadata(Long term, String voteFor) {
		this.term = term;
		this.voteFor = voteFor;
	}

	public static RaftMetadata getDefault() {
		return DEFAULT;
	}

}
