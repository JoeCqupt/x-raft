package io.github.xinfra.lab.raft.conf;

import io.github.xinfra.lab.raft.RaftPeerId;
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
	private  List<RaftPeerId> peers = new ArrayList<>();

	/**
	 * Learners
	 */
	private  List<RaftPeerId> learners = new ArrayList<>();

	public Configuration(List<RaftPeerId> peers, List<RaftPeerId> learners) {
		checkPeerIds(peers);
		checkPeerIds(learners);
		this.peers = peers;
		this.learners = learners;
	}


	public void checkPeerIds(List<RaftPeerId> peerIds) {
		// check duplicate peerId
		Validate.isTrue(peerIds.stream().map(RaftPeerId::getPeerId).distinct().count() == peerIds.size(),
				"peers can not have duplicate peerId");
		// check duplicate address
		Validate.isTrue(peerIds.stream().map(RaftPeerId::getAddress).distinct().count() == peerIds.size(),
				"peers can not have duplicate address");
	}



}
