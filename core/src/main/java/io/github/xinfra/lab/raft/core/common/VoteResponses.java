package io.github.xinfra.lab.raft.core.common;


import io.github.xinfra.lab.raft.common.RaftError;
import io.github.xinfra.lab.raft.protocol.ErrorInfo;
import io.github.xinfra.lab.raft.protocol.VoteResponse;

public class VoteResponses {

    public static VoteResponse failVoteResponse(RaftError error, String errorInfo) {
        VoteResponse voteResponse = new VoteResponse();
        voteResponse.setSuccess(false);
        voteResponse.setErrorInfo(new ErrorInfo(error.getCode(), errorInfo));
        return voteResponse;
    }

    public static VoteResponse voteResponse(boolean voteGranted, long term) {
        VoteResponse voteResponse = new VoteResponse();
        voteResponse.setSuccess(true);
        voteResponse.setVoteGranted(voteGranted);
        voteResponse.setTerm(term);
        return voteResponse;
    }

}
