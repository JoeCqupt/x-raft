package io.github.xinfra.lab.raft.core.state;

import io.github.xinfra.lab.raft.conf.Configuration;
import io.github.xinfra.lab.raft.conf.ConfigurationEntry;

import java.util.HashSet;
import java.util.Set;

public class BallotBox {

	private final ConfigurationEntry configurationEntry;

	Set<String> votedPeerIds = new HashSet<>();

	public BallotBox(ConfigurationEntry configurationEntry) {
		this.configurationEntry = configurationEntry;
	}

	public void grantVote(String peerId) {
		// fixme
		votedPeerIds.add(peerId);
	}

	public boolean isMajorityGranted() {
		return hasVotingPeersMajority(configurationEntry.getConf(), votedPeerIds)
				&& (configurationEntry.getOldConf() == null
						|| hasVotingPeersMajority(configurationEntry.getOldConf(), votedPeerIds));
	}

	public boolean hasVotingPeersMajority(Configuration conf, Set<String> peerIds) {
		return peerIds.size() > (conf.getPeers().size() / 2);
	}

}
