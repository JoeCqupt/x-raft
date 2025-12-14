package io.github.xinfra.lab.raft;

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

	List<RaftPeerId> peers;

	List<RaftPeerId> learners;

	private Long electionTimeoutMills = 150L;

	private Long electionTimeoutDelayMills = 150L;

	private Long rpcTimeoutMills = 100L;

	public void setPeers(List<RaftPeerId> peers) {
		// check duplicate peerId
		Validate.isTrue(peers.stream().map(RaftPeerId::getPeerId).distinct().count() == peers.size(),
				"peers can not have duplicate peerId");
		// check duplicate address
		Validate.isTrue(peers.stream().map(RaftPeerId::getAddress).distinct().count() == peers.size(),
				"peers can not have duplicate address");
		this.peers = peers;
	}

	public void setLearners(List<RaftPeerId> learners) {
		// check duplicate peerId
		Validate.isTrue(learners.stream().map(RaftPeerId::getPeerId).distinct().count() == learners.size(),
				"peers can not have duplicate peerId");
		// check duplicate address
		Validate.isTrue(learners.stream().map(RaftPeerId::getAddress).distinct().count() == learners.size(),
				"peers can not have duplicate address");
		this.learners = learners;
	}

}
