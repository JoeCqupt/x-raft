package io.github.xinfra.lab.raft.core;

import java.util.HashSet;
import java.util.Set;

public class BallotBox {

	private final RaftConfiguration raftConfiguration;

	Set<String> votedPeerIds = new HashSet<>();

	Set<String> rejectedPeerIds = new HashSet<>();

	public BallotBox(RaftConfiguration raftConfiguration) {
		this.raftConfiguration = raftConfiguration;
	}

	public void grantVote(String peerId) {
		votedPeerIds.add(peerId);
	}

	public void rejectVote(String peerId) {
		rejectedPeerIds.add(peerId);
	}

	public boolean isMajorityGranted() {
		return raftConfiguration.getConf().hasVotingPeersMajority(votedPeerIds)
				&& (raftConfiguration.getOldConf() == null
						|| raftConfiguration.getOldConf().hasVotingPeersMajority(votedPeerIds));
	}

	public boolean isMajorityRejected() {
		return raftConfiguration.getConf().hasVotingPeersMajority(rejectedPeerIds)
				&& (raftConfiguration.getOldConf() == null
						|| raftConfiguration.getOldConf().hasVotingPeersMajority(rejectedPeerIds));
	}

}
