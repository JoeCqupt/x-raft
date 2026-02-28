package io.github.xinfra.lab.raft.protocol;

public interface RaftAdminProtocol {

	/**
	 * change raft configuration
	 */
	SetConfigurationResponse setRaftConfiguration(SetConfigurationRequest request);

}
