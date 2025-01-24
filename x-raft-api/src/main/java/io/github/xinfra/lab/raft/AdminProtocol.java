package io.github.xinfra.lab.raft;

public interface AdminProtocol {

	SetConfigurationResponse setRaftConfiguration(SetConfigurationRequest request);

}
