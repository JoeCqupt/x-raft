package io.github.xinfra.lab.raft;

import lombok.Data;

import java.util.Set;

@Data
public class SetConfigurationRequest {

	private Set<RaftPeer> currentPeers;

	private Set<RaftPeer> newPeers;

}
