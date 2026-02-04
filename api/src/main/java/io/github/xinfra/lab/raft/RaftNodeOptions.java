package io.github.xinfra.lab.raft;

import io.github.xinfra.lab.raft.conf.Configuration;
import io.github.xinfra.lab.raft.log.RaftLogType;
import io.github.xinfra.lab.raft.statemachine.StateMachine;
import io.github.xinfra.lab.raft.transport.TransportClient;
import io.github.xinfra.lab.raft.transport.TransportClientOptions;
import io.github.xinfra.lab.raft.transport.TransportType;
import lombok.Data;

import java.util.concurrent.ThreadLocalRandom;

@Data
public class RaftNodeOptions {

	RaftPeer raftPeer;

	private boolean shareTransportClientFlag = false;

	// shared by all raft nodes
	// startup or shutdown by raft server
	private TransportClient shareTransportClient;

	// per raft node
	// startup or shutdown by per raft node
	private TransportType transportType;

	private TransportClientOptions transportClientOptions;

	private RaftLogType raftLogType;

	Configuration initialConf;

	StateMachine stateMachine;

	private Long electionTimeoutMills = 1500L;

	private Long electionTimeoutDelayMills = 500L;

	private Long rpcTimeoutMills = 500L;

	public Long getRandomElectionTimeoutMills() {
		return ThreadLocalRandom.current()
			.nextLong(electionTimeoutMills, electionTimeoutMills + electionTimeoutDelayMills);
	}

}
