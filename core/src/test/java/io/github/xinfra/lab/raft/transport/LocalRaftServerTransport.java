package io.github.xinfra.lab.raft.transport;

import io.github.xinfra.lab.raft.AbstractLifeCycle;
import io.github.xinfra.lab.raft.protocol.AppendEntriesRequest;
import io.github.xinfra.lab.raft.protocol.AppendEntriesResponse;
import io.github.xinfra.lab.raft.base.LocalXRaftNode;
import io.github.xinfra.lab.raft.RaftNode;
import io.github.xinfra.lab.raft.RaftPeerId;
import io.github.xinfra.lab.raft.protocol.VoteRequest;
import io.github.xinfra.lab.raft.protocol.VoteResponse;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * for unit test
 */
public class LocalRaftServerTransport extends AbstractLifeCycle implements RaftServerTransport {

	List<RaftNode> raftPeerNodes;

	Map<RaftPeerId, RaftNode> raftPeerNodeMap;

	Map<String, RaftPeerId> raftPeerMap;

	LocalXRaftNode localXRaftNode;

	public LocalRaftServerTransport(RaftNode raftNode) {
		if (!(raftNode instanceof LocalXRaftNode)) {
			throw new IllegalArgumentException("RaftNode must be LocalXRaftNode");
		}
		this.localXRaftNode = (LocalXRaftNode) raftNode;
	}

	@Override
	public void startup() {
		super.startup();
		this.raftPeerNodes = localXRaftNode.raftPeerNodes();
		this.raftPeerNodeMap = raftPeerNodes.stream().collect(Collectors.toMap(RaftNode::raftPeer, Function.identity()));
	}

	@Override
	public void addRaftPeers(Set<RaftPeerId> raftPeerIds) {
		raftPeerMap = raftPeerIds.stream().collect(Collectors.toMap(RaftPeerId::getPeerId, Function.identity()));
	}

	@Override
	public VoteResponse requestVote(VoteRequest voteRequest) {
		RaftPeerId raftPeerId = raftPeerMap.get(voteRequest.getReplyPeerId());
		RaftNode raftNode = raftPeerNodeMap.get(raftPeerId);
		return raftNode.requestVote(voteRequest);
	}

	@Override
	public AppendEntriesResponse appendEntries(AppendEntriesRequest appendEntriesRequest) {
		RaftPeerId raftPeerId = raftPeerMap.get(appendEntriesRequest.getReplyPeerId());
		RaftNode raftNode = raftPeerNodeMap.get(raftPeerId);
		return raftNode.appendEntries(appendEntriesRequest);
	}

}
