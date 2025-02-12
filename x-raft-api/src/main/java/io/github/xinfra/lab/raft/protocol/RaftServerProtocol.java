package io.github.xinfra.lab.raft.protocol;

public interface RaftServerProtocol {

	VoteResponse requestVote(VoteRequest voteRequest);

	AppendEntriesResponse appendEntries(AppendEntriesRequest appendEntriesRequest);

}
