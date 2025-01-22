package io.github.xinfra.lab.raft;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PeerConfiguration {

	/**
	 * Peers are voting members such as LEADER, CANDIDATE and FOLLOWER
	 */
	private final List<RaftPeer> peers;

	/**
	 * Listeners are non-voting members.
	 */
	private final List<RaftPeer> learners;

	public PeerConfiguration(List<RaftPeer> peers, List<RaftPeer> learners) {
		this.peers = peers;
		this.learners = learners;
	}
}
