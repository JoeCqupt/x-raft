package io.github.xinfra.lab.raft.core.common;

import io.github.xinfra.lab.raft.protocol.VoteResponse;

public class Responses {

	public static VoteResponse voteResponse(String requestPeerId, String replyPeerId, Long term, boolean voteGranted,
			boolean shouldShutdown) {
		VoteResponse voteResponse = new VoteResponse();
		voteResponse.setSuccess(true);
		voteResponse.setPeerId(requestPeerId);
		voteResponse.setReplyPeerId(replyPeerId);
		voteResponse.setTerm(term);
		voteResponse.setVoteGranted(voteGranted);
		voteResponse.setShouldShutdown(shouldShutdown);
		return voteResponse;
	}

}
