package io.github.xinfra.lab.raft.protocol;

public interface RaftProtocol {

	VoteResponse requestVote(VoteRequest voteRequest);

	AppendEntriesResponse appendEntries(AppendEntriesRequest appendEntriesRequest);

}
