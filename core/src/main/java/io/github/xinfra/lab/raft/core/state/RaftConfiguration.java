package io.github.xinfra.lab.raft.core.state;

import io.github.xinfra.lab.raft.RaftPeerId;
import io.github.xinfra.lab.raft.RaftRole;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class RaftConfiguration {

	private RaftPeerId selfRaftPeerId;

	/** Non-null only if this configuration is transitional. */
	private final PeerConfiguration oldConf;

	/**
	 * The current peer configuration while this configuration is stable; or the new peer
	 * configuration while this configuration is transitional.
	 */
	private final PeerConfiguration conf;

	/** The index of the corresponding log entry for this configuration. */
	private final Long logEntryIndex = -1L;

	public RaftConfiguration(RaftPeerId selfRaftPeerId, PeerConfiguration oldConf, PeerConfiguration conf) {
		this.selfRaftPeerId = selfRaftPeerId;
		this.oldConf = oldConf;
		this.conf = conf;
	}

	/**
	 * all voting raft peers
	 * @return
	 */
	public Set<RaftPeerId> getVotingRaftPeers() {
		Set<RaftPeerId> set = new HashSet<>();
		set.addAll(conf.getVotingPeers());
		if (oldConf != null) {
			set.addAll(oldConf.getVotingPeers());
		}
		return set;
	}

	/**
	 * all voting raft peers exclude raftPeerId
	 * @return
	 */
	public Set<RaftPeerId> getOtherVotingRaftPeers() {
		Set<RaftPeerId> set = getVotingRaftPeers();
		set.remove(selfRaftPeerId);
		return set;
	}

	/**
	 * all raft peers
	 * @return
	 */
	public Set<RaftPeerId> getRaftPeers() {
		Set<RaftPeerId> set = new HashSet<>();
		set.addAll(conf.getPeers());
		if (oldConf != null) {
			set.addAll(oldConf.getPeers());
		}
		return set;
	}

	/**
	 * all raft peers exclude raftPeerId
	 * @return
	 */
	public Set<RaftPeerId> getOtherRaftPeers() {
		Set<RaftPeerId> set = getRaftPeers();
		set.remove(selfRaftPeerId);
		return set;
	}

	/**
	 * @param candidateId
	 * @return maybe null
	 */
	public RaftPeerId getVotingRaftPeer(String candidateId) {
		RaftPeerId raftPeerId = getVotingRaftPeer(conf, candidateId);
		if (raftPeerId != null) {
			return raftPeerId;
		}
		return getVotingRaftPeer(oldConf, candidateId);
	}

	private RaftPeerId getVotingRaftPeer(PeerConfiguration conf, String candidateId) {
		RaftPeerId raftPeerId = conf.getRaftPeerMap().get(candidateId);
		if (raftPeerId != null && raftPeerId.getInitRole() != RaftRole.LEARNER) {
			return raftPeerId;
		}
		return null;
	}

}
