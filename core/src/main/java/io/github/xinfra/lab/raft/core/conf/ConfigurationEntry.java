package io.github.xinfra.lab.raft.core.conf;

import io.github.xinfra.lab.raft.RaftPeerId;
import io.github.xinfra.lab.raft.log.LogEntry;
import lombok.Data;

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

	public List<RaftPeerId> getPeers() {
		List<RaftPeerId> peers = conf.getPeers();
		if (oldConf != null) {
			peers.addAll(oldConf.getPeers());
		}
		return peers;
	}

	public List<RaftPeerId> getLearners() {
		List<RaftPeerId> learners = conf.getLearners();
		if (oldConf != null) {
			learners.addAll(oldConf.getLearners());
		}
		return learners;
	}

	// todo
	public RaftPeerId getRaftPeerId(String peerId) {
		RaftPeerId peer = conf.getPeers().stream().filter(p -> p.getPeerId().equals(peerId)).findFirst().get();
		if (peer != null) {
			return peer;
		}
		if (oldConf != null) {
			peer = oldConf.getPeers().stream().filter(p -> p.getPeerId().equals(peerId)).findFirst().get();
		}
		return peer;
	}

}
