package io.github.xinfra.lab.raft;

import io.github.xinfra.lab.raft.transport.TransportType;
import lombok.Data;

@Data
public class RaftNodeConfig {

	private TransportType transportType;

}
