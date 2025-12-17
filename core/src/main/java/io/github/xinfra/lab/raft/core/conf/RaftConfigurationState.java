package io.github.xinfra.lab.raft.core.conf;

import io.github.xinfra.lab.raft.RaftPeerId;
import io.github.xinfra.lab.raft.conf.ConfigurationEntry;
import lombok.Getter;

import java.util.LinkedList;


public class RaftConfigurationState {

	private ConfigurationEntry initialConfiguration;

	@Getter
	private ConfigurationEntry currentConfiguration;

	private LinkedList<ConfigurationEntry> configurations = new LinkedList<>();

	public RaftConfigurationState(ConfigurationEntry initialConfiguration) {
		this.initialConfiguration = initialConfiguration;
		// set initial configuration as current
		this.currentConfiguration = initialConfiguration;
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
		this.currentConfiguration = (configurations.getLast());
	}

	public RaftPeerId getRaftPeerId(String peerId) {
		return currentConfiguration.getRaftPeerId(peerId);
	}

}
