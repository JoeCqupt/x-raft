package io.github.xinfra.lab.raft;

public interface RaftServerProtocol {
    RequestVoteResponse requestVote(RequestVoteRequest requestVoteRequest);
}
