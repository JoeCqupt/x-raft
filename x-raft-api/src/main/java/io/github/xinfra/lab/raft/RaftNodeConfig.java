package io.github.xinfra.lab.raft;

import lombok.Data;

@Data
public class RaftNodeConfig {

	private TransportType transportType;

	private long electionTimeoutMills = 1000L;

	private long electionTimeoutDelayMills = 1000L;

	private long rpcTimeoutMills = 3000L;

}
