package io.github.xinfra.lab.raft.protocol;

public interface RaftAdminService {

	/**
	 * change raft configuration
	 */
	SetConfigurationResponse setRaftConfiguration(SetConfigurationRequest request);

}
