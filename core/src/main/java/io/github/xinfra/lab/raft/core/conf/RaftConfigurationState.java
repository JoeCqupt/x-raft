package io.github.xinfra.lab.raft.core.conf;

import io.github.xinfra.lab.raft.RaftPeer;
import io.github.xinfra.lab.raft.conf.ConfigurationEntry;
import lombok.Getter;

import java.util.LinkedList;


public class RaftConfigurationState {

	private ConfigurationEntry initialConfig;

	@Getter
	private ConfigurationEntry currentConfig;

	private LinkedList<ConfigurationEntry> configurations = new LinkedList<>();

	public RaftConfigurationState(ConfigurationEntry initialConfig) {
		this.initialConfig = initialConfig;
		// set initial configuration as current
		this.currentConfig = initialConfig;
	}

	public boolean addConfiguration(ConfigurationEntry entry) {
		// todo @joecqupt
		return true;
	}

	/**
	 * check if there is any new configuration and set it as current
	 */
	public void updateConfiguration() {
		if (configurations.isEmpty()) {
			return;
		}
		this.currentConfig = (configurations.getLast());
	}


}
