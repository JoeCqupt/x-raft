package io.github.xinfra.lab.raft;

import io.github.xinfra.lab.raft.log.RaftLogType;
import io.github.xinfra.lab.raft.transport.TransportType;
import lombok.Data;


@Data
public class RaftNodeOptions {

	private TransportType transportType;

	private RaftLogType raftLogType;

	private Long electionTimeoutMills = 150L;

	private Long electionTimeoutDelayMills = 150L;

	private Long rpcTimeoutMills = 100L;

}
