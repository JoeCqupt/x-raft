package io.github.xinfra.lab.raft;

import lombok.Getter;

public class RaftConfiguration {

	/** Non-null only if this configuration is transitional. */
	@Getter
	private final PeerConfiguration oldConf;

	/**
	 * The current peer configuration while this configuration is stable; or the new peer
	 * configuration while this configuration is transitional.
	 */
	@Getter
	private final PeerConfiguration conf;

	/** The index of the corresponding log entry for this configuration. */
	@Getter
	private final long logEntryIndex;

	public RaftConfiguration(PeerConfiguration oldConf, PeerConfiguration conf, long logEntryIndex) {
		this.oldConf = oldConf;
		this.conf = conf;
		this.logEntryIndex = logEntryIndex;
	}

}
