package io.github.xinfra.lab.raft.protocol;

public interface AdminProtocol {

	SetConfigurationResponse setRaftConfiguration(SetConfigurationRequest request);

}
