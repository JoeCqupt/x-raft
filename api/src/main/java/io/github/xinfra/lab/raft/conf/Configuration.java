package io.github.xinfra.lab.raft.conf;

import io.github.xinfra.lab.raft.RaftPeer;
import lombok.Data;
import org.apache.commons.lang3.Validate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class Configuration implements Serializable {

	/**
	 * All peers in the raft group. LEADER, CANDIDATE, FOLLOWER
	 */
	private  List<RaftPeer> peers = new ArrayList<>();

	/**
	 * Learners
	 */
	private  List<RaftPeer> learners = new ArrayList<>();

	public Configuration(List<RaftPeer> peers, List<RaftPeer> learners) {
		checkPeerIds(peers);
		checkPeerIds(learners);
		this.peers = peers;
		this.learners = learners;
	}


	public void checkPeerIds(List<RaftPeer> peerIds) {
		// check duplicate peerId
		Validate.isTrue(peerIds.stream().map(RaftPeer::getRaftPeerId).distinct().count() == peerIds.size(),
				"peers can not have duplicate peerId");
		// check duplicate address
		Validate.isTrue(peerIds.stream().map(RaftPeer::getAddress).distinct().count() == peerIds.size(),
				"peers can not have duplicate address");
	}



}
