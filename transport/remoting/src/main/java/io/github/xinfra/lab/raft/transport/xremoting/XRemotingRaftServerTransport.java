package io.github.xinfra.lab.raft.transport.xremoting;

import io.github.xinfra.lab.raft.AbstractLifeCycle;
import io.github.xinfra.lab.raft.protocol.AppendEntriesRequest;
import io.github.xinfra.lab.raft.protocol.AppendEntriesResponse;
import io.github.xinfra.lab.raft.RaftNode;
import io.github.xinfra.lab.raft.RaftPeerId;
import io.github.xinfra.lab.raft.protocol.VoteRequest;
import io.github.xinfra.lab.raft.protocol.VoteResponse;
import io.github.xinfra.lab.raft.transport.RaftServerTransport;

import java.util.Set;

public class XRemotingRaftServerTransport extends AbstractLifeCycle implements RaftServerTransport {

	private RaftNode raftNode;

	public XRemotingRaftServerTransport(RaftNode raftNode) {
		this.raftNode = raftNode;
	}

	@Override
	public void addRaftPeers(Set<RaftPeerId> raftPeerIds) {
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
