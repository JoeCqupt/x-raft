package io.github.xinfra.lab.raft.core.state;

import lombok.Data;

@Data
public class RaftConfigurationState {

	private RaftConfiguration initialConfiguration;

	private RaftConfiguration currentConfiguration;

	// todo
	private Long logIndex;

	public RaftConfigurationState(RaftConfiguration initialConfiguration) {
		this.initialConfiguration = initialConfiguration;
		setCurrentConfiguration(initialConfiguration);
	}

}
