package io.github.xinfra.lab.raft.base;

import io.github.xinfra.lab.raft.core.XRaftGroup;
import io.github.xinfra.lab.raft.core.XRaftNode;
import io.github.xinfra.lab.raft.statemachine.NOOPStateMachine;
import io.github.xinfra.lab.raft.core.RaftGroupOptions;
import io.github.xinfra.lab.raft.core.RaftNodeOptions;
import io.github.xinfra.lab.raft.RaftPeer;
import io.github.xinfra.lab.raft.RaftRole;
import io.github.xinfra.lab.raft.core.RaftServerOptions;
import io.github.xinfra.lab.raft.conf.Configuration;
import io.github.xinfra.lab.raft.core.XRaftServer;
import io.github.xinfra.lab.raft.core.log.MemoryRaftLogType;
import io.github.xinfra.lab.raft.transport.LocalTransportClient;
import io.github.xinfra.lab.raft.transport.LocalTransportType;
import io.github.xinfra.lab.raft.transport.TransportClient;
import io.github.xinfra.lab.raft.transport.TransportClientOptions;
import io.github.xinfra.lab.raft.transport.TransportServerOptions;
import io.github.xinfra.lab.raft.transport.TransportType;
import lombok.Getter;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 1 raft group + N raft server + N raft nodes
 */
@Getter
public class TestCluster {

	private String raftGroupId;

	private static String nodeIdPrefix = "node-";

	private String serverIp = "localhost";

	private int serverPort = 6666;

	private TransportType transportType = LocalTransportType.local;

	private int peerNums;

	List<RaftPeer> raftPeers = new ArrayList<>();

	List<XRaftServer> raftServers = new ArrayList<>();

	List<XRaftNode> raftNodes = new ArrayList<>();

	Map<XRaftNode, XRaftServer> raftNodeRaftServerMap = new HashMap<>();

	public TestCluster(String raftGroupId, int peerNums) {
		this.raftGroupId = raftGroupId;
		this.peerNums = peerNums;
	}

	public void startup() {
		// generate raft peer ids
		raftPeers = new ArrayList<>();
		for (int i = 0; i < peerNums; i++) {
			RaftPeer raftPeer = new RaftPeer();
			raftPeer.setRaftPeerId(nodeIdPrefix + i);
			raftPeer.setAddress(new InetSocketAddress(serverIp, serverPort + i));
			raftPeers.add(raftPeer);
		}

		// start raft servers
		for (int i = 0; i < peerNums; i++) {
			RaftPeer raftPeer = raftPeers.get(i);

			RaftServerOptions raftServerOptions = new RaftServerOptions();
			raftServerOptions.setTransportType(transportType);
			TransportServerOptions transportServerOptions = new TransportServerOptions();
			transportServerOptions.setIp(raftPeer.getAddress().getHostName());
			transportServerOptions.setPort(raftPeer.getAddress().getPort());
			raftServerOptions.setTransportServerOptions(transportServerOptions);
			XRaftServer raftServer = new XRaftServer(raftServerOptions);
			raftServer.startup();

			raftServers.add(raftServer);
		}

		// create transport client
		TransportClient transportClient = createTransportClient();

		// every raft server start raft group
		for (int i = 0; i < peerNums; i++) {
			XRaftServer raftServer = raftServers.get(i);
			RaftPeer raftPeer = raftPeers.get(i);

			RaftNodeOptions raftNodeOptions = new RaftNodeOptions();
			raftNodeOptions.setRaftPeer(raftPeer);
			raftNodeOptions.setShareTransportClientFlag(true);
			raftNodeOptions.setShareTransportClient(transportClient);
			raftNodeOptions.setInitialConf(new Configuration(raftPeers, new ArrayList<>()));
			raftNodeOptions.setRaftLogType(MemoryRaftLogType.memory);
			raftNodeOptions.setStateMachine(new NOOPStateMachine());

			RaftGroupOptions raftGroupOptions = new RaftGroupOptions();
			raftGroupOptions.setRaftNodeOptions(raftNodeOptions);
			raftGroupOptions.setRaftGroupId(raftGroupId);

			XRaftGroup raftGroup = raftServer.startRaftGroup(raftGroupOptions);

			raftNodes.add(raftGroup.getRaftNode());
			raftNodeRaftServerMap.put(raftGroup.getRaftNode(), raftServer);
		}

	}

	public void shutdown() {
		for (XRaftServer raftServer : raftServers) {
			raftServer.shutdown();
		}
	}

	private TransportClient createTransportClient() {
		TransportClient transportClient = transportType.newClient(new TransportClientOptions());
		if (transportType == LocalTransportType.local) {
			LocalTransportClient localTransportClient = (LocalTransportClient) transportClient;
			localTransportClient.setRaftCluster(this);
		}

		return transportClient;
	}

	public XRaftNode getLeaderNode() {
		for (XRaftNode raftNode : raftNodes) {
			if (raftNode.getRaftRole() == RaftRole.LEADER) {
				return raftNode;
			}
		}
		return null;
	}

	public String printRaftNodes() {
		StringBuilder sb = new StringBuilder();
		for (XRaftNode raftNode : raftNodes) {
			sb.append(raftNode.getRaftGroupPeerId())
				.append(":")
				.append(raftNode.getRaftRole())
				.append(System.lineSeparator());
		}
		return sb.toString();
	}

}
