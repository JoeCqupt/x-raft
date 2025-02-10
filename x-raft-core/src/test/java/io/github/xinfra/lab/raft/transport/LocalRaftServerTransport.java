package io.github.xinfra.lab.raft.transport;

import io.github.xinfra.lab.raft.AbstractLifeCycle;
import io.github.xinfra.lab.raft.AppendEntriesRequest;
import io.github.xinfra.lab.raft.AppendEntriesResponse;
import io.github.xinfra.lab.raft.LocalXRaftNode;
import io.github.xinfra.lab.raft.RaftNode;
import io.github.xinfra.lab.raft.RaftPeer;
import io.github.xinfra.lab.raft.RaftServerTransport;
import io.github.xinfra.lab.raft.VoteRequest;
import io.github.xinfra.lab.raft.VoteResponse;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LocalRaftServerTransport extends AbstractLifeCycle implements RaftServerTransport {

	List<RaftNode> raftNodes;

	Map<RaftPeer, RaftNode> raftNodeMap;

	Map<String, RaftPeer> raftPeerMap;

	public LocalRaftServerTransport(RaftNode raftNode) {
		if (!(raftNode instanceof LocalXRaftNode)) {
			throw new IllegalArgumentException("RaftNode must be LocalXRaftNode");
		}
		LocalXRaftNode localRaftNode = (LocalXRaftNode) raftNode;

		this.raftNodes = localRaftNode.otherRaftNodes();
		this.raftNodeMap = raftNodes.stream().collect(Collectors.toMap(RaftNode::self, Function.identity()));
		addRaftPeers(raftNodeMap.keySet());
	}

	@Override
	public void addRaftPeers(Set<RaftPeer> raftPeers) {
		raftPeerMap = raftPeers.stream().collect(Collectors.toMap(RaftPeer::getRaftPeerId, Function.identity()));
	}

	@Override
	public VoteResponse requestVote(VoteRequest voteRequest) {
		RaftPeer raftPeer = raftPeerMap.get(voteRequest.getReplyPeerId());
		RaftNode raftNode = raftNodeMap.get(raftPeer);
		return raftNode.requestVote(voteRequest);
	}

	@Override
	public AppendEntriesResponse appendEntries(AppendEntriesRequest appendEntriesRequest) {
		RaftPeer raftPeer = raftPeerMap.get(appendEntriesRequest.getReplyPeerId());
		RaftNode raftNode = raftNodeMap.get(raftPeer);
		return raftNode.appendEntries(appendEntriesRequest);
	}

}
