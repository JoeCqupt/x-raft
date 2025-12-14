package io.github.xinfra.lab.raft.core.conf;

import lombok.Data;

import java.util.LinkedList;

@Data
public class RaftConfigurationState {

	private ConfigurationEntry initialConfiguration;

	private ConfigurationEntry currentConfiguration;

	private LinkedList<ConfigurationEntry> configurations = new LinkedList<>();

	public RaftConfigurationState(ConfigurationEntry initialConfiguration) {
		this.initialConfiguration = initialConfiguration;
		// set initial configuration as current
		setCurrentConfiguration(initialConfiguration);
	}

	public boolean addConfiguration(ConfigurationEntry entry) {
		// todo @joecqupt
		return true;
	}

	/**
	 * check if there is any new configuration and set it as current
	 */
	public void checkAndSetCurrentConfiguration() {
		if (configurations.isEmpty()) {
			return;
		}
		setCurrentConfiguration(configurations.getLast());
	}

}
