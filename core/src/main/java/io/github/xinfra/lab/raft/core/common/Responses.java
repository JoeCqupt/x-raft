package io.github.xinfra.lab.raft.core.common;

import io.github.xinfra.lab.raft.protocol.VoteResponse;

public class Responses {

	public static VoteResponse voteResponse(boolean voteGranted, long term) {
		VoteResponse voteResponse = new VoteResponse();
		voteResponse.setVoteGranted(voteGranted);
		voteResponse.setTerm(term);
		return voteResponse;
	}

}
