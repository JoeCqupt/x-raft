package io.github.xinfra.lab.raft.core.state;

import io.github.xinfra.lab.raft.RaftPeer;
import io.github.xinfra.lab.raft.RaftRole;
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
	private final Long logEntryIndex = -1L;

	public RaftConfiguration(RaftPeer selfRaftPeer, PeerConfiguration oldConf, PeerConfiguration conf) {
		this.selfRaftPeer = selfRaftPeer;
		this.oldConf = oldConf;
		this.conf = conf;
	}

	/**
	 * all voting raft peers
	 * @return
	 */
	public Set<RaftPeer> getVotingRaftPeers() {
		Set<RaftPeer> set = new HashSet<>();
		set.addAll(conf.getVotingPeers());
		if (oldConf != null) {
			set.addAll(oldConf.getVotingPeers());
		}
		return set;
	}

	/**
	 * all voting raft peers exclude raftPeer
	 * @return
	 */
	public Set<RaftPeer> getOtherVotingRaftPeers() {
		Set<RaftPeer> set = getVotingRaftPeers();
		set.remove(selfRaftPeer);
		return set;
	}

	/**
	 * all raft peers
	 * @return
	 */
	public Set<RaftPeer> getRaftPeers() {
		Set<RaftPeer> set = new HashSet<>();
		set.addAll(conf.getPeers());
		if (oldConf != null) {
			set.addAll(oldConf.getPeers());
		}
		return set;
	}

	/**
	 * all raft peers exclude raftPeer
	 * @return
	 */
	public Set<RaftPeer> getOtherRaftPeers() {
		Set<RaftPeer> set = getRaftPeers();
		set.remove(selfRaftPeer);
		return set;
	}

	/**
	 * @param candidateId
	 * @return maybe null
	 */
    public RaftPeer getVotingRaftPeer(String candidateId) {
        RaftPeer raftPeer = getVotingRaftPeer(conf, candidateId);
        if (raftPeer != null) {
            return raftPeer;
        }
        return getVotingRaftPeer(oldConf, candidateId);
    }

	private RaftPeer getVotingRaftPeer(PeerConfiguration conf, String candidateId) {
		RaftPeer raftPeer = conf.getRaftPeerMap().get(candidateId);
		if (raftPeer != null && raftPeer.getInitRole() != RaftRole.LEARNER) {
			return raftPeer;
		}
		return null;
	}

}
