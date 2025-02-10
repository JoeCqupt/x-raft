package io.github.xinfra.lab.raft;

import lombok.Data;

@Data
public class RaftNodeConfig {

	private TransportType transportType;

	private long electionTimeoutMills = 150L;

	private long electionTimeoutDelayMills = 150L;

	private long rpcTimeoutMills = 100L;

}
