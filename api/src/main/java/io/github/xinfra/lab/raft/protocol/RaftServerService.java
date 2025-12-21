package io.github.xinfra.lab.raft.protocol;

public interface RaftServerService {

	VoteResponse handlePreVoteRequest(VoteRequest voteRequest);

	VoteResponse handleVoteRequest(VoteRequest voteRequest);

	AppendEntriesResponse handleAppendEntries(AppendEntriesRequest appendEntriesRequest);

}
