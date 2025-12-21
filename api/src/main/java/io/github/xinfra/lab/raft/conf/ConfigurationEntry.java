package io.github.xinfra.lab.raft.conf;

import io.github.xinfra.lab.raft.RaftPeer;
import io.github.xinfra.lab.raft.log.LogEntry;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ConfigurationEntry implements LogEntry {

	/**
	 * Non-null only if this configuration is transitional.
	 */
	private final Configuration oldConf;

	/**
	 * The current peer configuration while this configuration is stable; or the new peer
	 * configuration while this configuration is transitional.
	 */
	private final Configuration conf;

	/**
	 * The index of the corresponding log entry for this configuration.
	 */
	private final Long logEntryIndex = -1L;

	private final Long logEntryTerm = -1L;

	public ConfigurationEntry(Configuration oldConf, Configuration conf) {
		this.oldConf = oldConf;
		this.conf = conf;
	}

	@Override
	public Long index() {
		return logEntryIndex;
	}

	@Override
	public Long term() {
		return logEntryTerm;
	}

	public List<RaftPeer> getPeers() {
		List<RaftPeer> peers = new ArrayList<>(conf.getPeers());
		if (oldConf != null) {
			peers.addAll(oldConf.getPeers());
		}
		return peers;
	}

	public List<RaftPeer> getLearners() {
		List<RaftPeer> learners = new ArrayList<>(conf.getLearners());
		if (oldConf != null) {
			learners.addAll(oldConf.getLearners());
		}
		return learners;
	}

	// todo
	public RaftPeer getRaftPeer(String peerId) {
		RaftPeer peer = conf.getPeers().stream().filter(p -> p.getRaftPeerId().equals(peerId)).findFirst().get();
		if (peer != null) {
			return peer;
		}
		if (oldConf != null) {
			peer = oldConf.getPeers().stream().filter(p -> p.getRaftPeerId().equals(peerId)).findFirst().get();
		}
		return peer;
	}

}
