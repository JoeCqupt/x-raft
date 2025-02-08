package io.github.xinfra.lab.raft.transport;

import io.github.xinfra.lab.raft.AbstractLifeCycle;
import io.github.xinfra.lab.raft.AppendEntriesRequest;
import io.github.xinfra.lab.raft.AppendEntriesResponse;
import io.github.xinfra.lab.raft.RaftNode;
import io.github.xinfra.lab.raft.RaftPeer;
import io.github.xinfra.lab.raft.RaftServerTransport;
import io.github.xinfra.lab.raft.VoteRequest;
import io.github.xinfra.lab.raft.VoteResponse;

import java.util.Set;

public class XRaftServerTransport extends AbstractLifeCycle implements RaftServerTransport {

	private RaftNode raftNode;

	public XRaftServerTransport(RaftNode raftNode) {
		this.raftNode = raftNode;
	}

	@Override
	public void addRaftPeers(Set<RaftPeer> raftPeers) {
		// todo
	}

	@Override
	public VoteResponse requestVote(VoteRequest voteRequest) {
		Long timeoutMills = raftNode.getRaftNodeConfig().getRpcTimeoutMills();
		// todo
		return null;
	}

	@Override
	public AppendEntriesResponse appendEntries(AppendEntriesRequest appendEntriesRequest) {
		// todo
		return null;
	}

	@Override
	public void startup() {
		// todo start server
		super.startup();
	}

	@Override
	public void shutdown() {
		// todo stop server
		super.shutdown();
	}

}
