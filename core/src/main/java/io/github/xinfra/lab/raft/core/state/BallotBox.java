package io.github.xinfra.lab.raft.core.state;

import io.github.xinfra.lab.raft.core.conf.ConfigurationEntry;

import java.util.HashSet;
import java.util.Set;

public class BallotBox {

	private final ConfigurationEntry configurationEntry;

	Set<String> votedPeerIds = new HashSet<>();

	Set<String> rejectedPeerIds = new HashSet<>();

	public BallotBox(ConfigurationEntry configurationEntry) {
		this.configurationEntry = configurationEntry;
	}

	public void grantVote(String peerId) {
		votedPeerIds.add(peerId);
	}

	public void rejectVote(String peerId) {
		rejectedPeerIds.add(peerId);
	}

	public boolean isMajorityGranted() {
		return configurationEntry.getConf().hasVotingPeersMajority(votedPeerIds)
				&& (configurationEntry.getOldConf() == null
						|| configurationEntry.getOldConf().hasVotingPeersMajority(votedPeerIds));
	}

	public boolean isMajorityRejected() {
		return configurationEntry.getConf().hasVotingPeersMajority(rejectedPeerIds)
				&& (configurationEntry.getOldConf() == null
						|| configurationEntry.getOldConf().hasVotingPeersMajority(rejectedPeerIds));
	}

}
