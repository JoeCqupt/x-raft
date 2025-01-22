package io.github.xinfra.lab.raft;

import lombok.Getter;

public class RaftConfigurationManager {

	private String raftPeerId;
	@Getter
	private RaftConfiguration initialConfiguration;

	private RaftConfiguration currentConfiguration;


	public RaftConfigurationManager(String raftPeerId, RaftConfiguration initialConfiguration) {
		this.raftPeerId = raftPeerId;
		this.initialConfiguration = initialConfiguration;
		setCurrentConfiguration(initialConfiguration);
	}

	public void setCurrentConfiguration(RaftConfiguration configuration) {
		this.currentConfiguration = configuration;
	}
}
