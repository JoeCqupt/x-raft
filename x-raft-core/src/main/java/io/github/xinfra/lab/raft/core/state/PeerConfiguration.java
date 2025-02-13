package io.github.xinfra.lab.raft.core.state;

import io.github.xinfra.lab.raft.RaftPeer;
import io.github.xinfra.lab.raft.RaftRole;
import lombok.Data;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
public class PeerConfiguration {

	/**
	 * All peers in the raft group. LEADER, CANDIDATE, FOLLOWER and Listener
	 */
	private final Set<RaftPeer> peers;

	/**
	 * Peers are voting members such as LEADER, CANDIDATE and FOLLOWER
	 */
	private final Set<RaftPeer> votingPeers;

	/**
	 * Listeners are non-voting members.
	 */
	private final Set<RaftPeer> nonVotingPeers;

	/**
	 * All peers in raft group.
	 */
	private final Map<String, RaftPeer> raftPeerMap;

	public PeerConfiguration(Set<RaftPeer> peers) {
		this.peers = peers;
		this.raftPeerMap = peers.stream().collect(Collectors.toMap(RaftPeer::getRaftPeerId, Function.identity()));

		this.votingPeers = peers.stream().filter(p -> p.getRole() != RaftRole.LEARNER).collect(Collectors.toSet());
		this.nonVotingPeers = peers.stream().filter(p -> p.getRole() == RaftRole.LEARNER).collect(Collectors.toSet());
	}

	public boolean hasVotingPeersMajority(Set<String> peerIds) {
		return votingPeers.stream()
			.filter(peer -> peerIds.contains(peer.getRaftPeerId()))
			.count() > (votingPeers.size() / 2);
	}

}
