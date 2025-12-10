package io.github.xinfra.lab.raft.core.state;

import io.github.xinfra.lab.raft.RaftPeer;
import io.github.xinfra.lab.raft.RaftRole;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
public class PeerConfiguration {

	/**
	 * All peers in the raft group. LEADER, CANDIDATE, FOLLOWER and Listener
	 */
	private final List<RaftPeer> peers;

	/**
	 * Peers are voting members such as LEADER, CANDIDATE and FOLLOWER
	 */
	private final List<RaftPeer> votingPeers;

	/**
	 * Learners are non-voting members.
	 */
	private final List<RaftPeer> nonVotingPeers;

	/**
	 * All peers in raft group.
	 */
	private final Map<String, RaftPeer> raftPeerMap;

	public PeerConfiguration(List<RaftPeer> peers) {
		this.peers = peers;
		this.raftPeerMap = peers.stream().collect(Collectors.toMap(RaftPeer::getRaftPeerId, Function.identity()));

		this.votingPeers = peers.stream().filter(p -> p.getRole() != RaftRole.LEARNER).collect(Collectors.toList());
		this.nonVotingPeers = peers.stream().filter(p -> p.getRole() == RaftRole.LEARNER).collect(Collectors.toList());
	}

	public boolean hasVotingPeersMajority(Set<String> peerIds) {
		return votingPeers.stream()
			.filter(peer -> peerIds.contains(peer.getRaftPeerId()))
			.count() > (votingPeers.size() / 2);
	}

}
