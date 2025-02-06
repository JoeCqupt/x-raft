package io.github.xinfra.lab.raft;

import io.github.xinfra.lab.raft.transport.TransportType;
import lombok.Data;

@Data
public class RaftNodeConfig {

	private TransportType transportType;

	private Long minRpcTimeoutMills = 100L;

	private Long maxRpcTimeoutMills = 300L;

}
