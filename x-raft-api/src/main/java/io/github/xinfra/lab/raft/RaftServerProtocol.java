package io.github.xinfra.lab.raft;

public interface RaftServerProtocol {

	VoteResponse requestVote(VoteRequest voteRequest);

}
