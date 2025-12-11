package io.github.xinfra.lab.raft.protocol;

import io.github.xinfra.lab.raft.RaftPeerId;
import lombok.Data;

import java.util.Set;

@Data
public class SetConfigurationRequest {

	private Set<RaftPeerId> currentPeers;

	private Set<RaftPeerId> newPeers;

}
