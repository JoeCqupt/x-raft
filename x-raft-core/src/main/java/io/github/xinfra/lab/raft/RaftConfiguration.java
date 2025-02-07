package io.github.xinfra.lab.raft;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class RaftConfiguration {

	private RaftPeer selfRaftPeer;

	/** Non-null only if this configuration is transitional. */
	private final PeerConfiguration oldConf;

	/**
	 * The current peer configuration while this configuration is stable; or the new peer
	 * configuration while this configuration is transitional.
	 */
	private final PeerConfiguration conf;

	/** The index of the corresponding log entry for this configuration. */
	private final long logEntryIndex = -1;

	public RaftConfiguration(RaftPeer selfRaftPeer, PeerConfiguration oldConf, PeerConfiguration conf) {
		this.selfRaftPeer = selfRaftPeer;
		this.oldConf = oldConf;
		this.conf = conf;
	}

	public Set<RaftPeer> getVotingRaftPeers() {
		Set<RaftPeer> set = new HashSet<>();
		set.addAll(conf.getVotingPeers());
		if (oldConf != null) {
			set.addAll(oldConf.getVotingPeers());
		}
		set.remove(selfRaftPeer);
		return set;
	}

	public Set<RaftPeer> getRaftPeers() {
		Set<RaftPeer> set = new HashSet<>();
		set.addAll(conf.getPeers());
		if (oldConf != null) {
			set.addAll(oldConf.getPeers());
		}
		set.remove(selfRaftPeer);
		return set;
	}

}
