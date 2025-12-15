package io.github.xinfra.lab.raft.core.state;

import io.github.xinfra.lab.raft.core.conf.Configuration;
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
		return hasVotingPeersMajority(configurationEntry.getConf(), votedPeerIds)
				&& (configurationEntry.getOldConf() == null
						|| hasVotingPeersMajority(configurationEntry.getOldConf(), votedPeerIds));
	}

	public boolean isMajorityRejected() {
		return hasVotingPeersMajority(configurationEntry.getConf(), rejectedPeerIds)
				&& (configurationEntry.getOldConf() == null
						|| hasVotingPeersMajority(configurationEntry.getOldConf(), rejectedPeerIds));
	}

	public boolean hasVotingPeersMajority(Configuration conf, Set<String> peerIds) {
		return peerIds.size() > (conf.getPeers().size() / 2);
	}

}
