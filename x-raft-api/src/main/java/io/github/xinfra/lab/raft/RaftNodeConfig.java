package io.github.xinfra.lab.raft;

import io.github.xinfra.lab.raft.log.RaftLogType;
import io.github.xinfra.lab.raft.transport.TransportType;
import lombok.Data;

@Data
public class RaftNodeConfig {

	private TransportType transportType;

	private RaftLogType raftLogType;

	private long electionTimeoutMills = 150L;

	private long electionTimeoutDelayMills = 150L;

	private long rpcTimeoutMills = 100L;

}
