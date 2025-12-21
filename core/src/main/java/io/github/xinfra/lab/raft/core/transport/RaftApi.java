package io.github.xinfra.lab.raft.core.transport;

import io.github.xinfra.lab.raft.transport.RequestApi;

public enum RaftApi implements RequestApi {

	requestVote("/raft/handleVoteRequest"), appendEntries("/raft/handleAppendEntries");

	private String path;

	RaftApi(String path) {
		this.path = path;
	}

	@Override
	public String path() {
		return path;
	}

}
