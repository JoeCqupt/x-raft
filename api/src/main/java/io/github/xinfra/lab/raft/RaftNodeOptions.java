package io.github.xinfra.lab.raft;

import io.github.xinfra.lab.raft.conf.Configuration;
import io.github.xinfra.lab.raft.log.RaftLogType;
import io.github.xinfra.lab.raft.transport.TransportClient;
import io.github.xinfra.lab.raft.transport.TransportClientOptions;
import io.github.xinfra.lab.raft.transport.TransportType;
import lombok.Data;
import org.apache.commons.lang3.Validate;

import java.util.List;

@Data
public class RaftNodeOptions {

	RaftPeerId raftPeerId;

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

	private Long electionTimeoutMills = 150L;

	private Long electionTimeoutDelayMills = 150L;

	private Long rpcTimeoutMills = 100L;

}
