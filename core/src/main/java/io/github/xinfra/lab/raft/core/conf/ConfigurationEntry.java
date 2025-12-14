package io.github.xinfra.lab.raft.core.conf;

import io.github.xinfra.lab.raft.log.LogEntry;
import lombok.Data;

@Data
public class ConfigurationEntry implements LogEntry {

	/** Non-null only if this configuration is transitional. */
	private final Configuration oldConf;

	/**
	 * The current peer configuration while this configuration is stable; or the new peer
	 * configuration while this configuration is transitional.
	 */
	private final Configuration conf;

	/** The index of the corresponding log entry for this configuration. */
	private final Long logEntryIndex = -1L;

	private final Long logEntryTerm = -1L;

	public ConfigurationEntry(Configuration oldConf, Configuration conf) {
		this.oldConf = oldConf;
		this.conf = conf;
	}

	@Override
	public Long index() {
		// todo @joecqupt
		return 0L;
	}

	@Override
	public Long term() {
		// todo @joecqupt
		return 0L;
	}

}
