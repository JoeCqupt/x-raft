package io.github.xinfra.lab.raft.base;

import io.github.xinfra.lab.raft.RaftGroup;
import io.github.xinfra.lab.raft.RaftGroupOptions;
import io.github.xinfra.lab.raft.RaftNode;
import io.github.xinfra.lab.raft.RaftNodeOptions;
import io.github.xinfra.lab.raft.RaftPeer;
import io.github.xinfra.lab.raft.RaftRole;
import io.github.xinfra.lab.raft.RaftServer;
import io.github.xinfra.lab.raft.RaftServerOptions;
import io.github.xinfra.lab.raft.conf.Configuration;
import io.github.xinfra.lab.raft.core.XRaftServer;
import io.github.xinfra.lab.raft.core.log.MemoryRaftLogType;
import io.github.xinfra.lab.raft.transport.LocalTransportClient;
import io.github.xinfra.lab.raft.transport.LocalTransportType;
import io.github.xinfra.lab.raft.transport.TransportClient;
import io.github.xinfra.lab.raft.transport.TransportClientOptions;
import io.github.xinfra.lab.raft.transport.TransportServerOptions;
import io.github.xinfra.lab.raft.transport.TransportType;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * 1 raft group + N raft server + N raft nodes
 */
public class TestCluster {

    private String raftGroupId;

    private static String nodeIdPrefix = "node-";

    private String serverIp = "localhost";

    private int serverPort = 6666;

    private TransportType transportType = LocalTransportType.local;

    private int peerNums;

    List<RaftPeer> raftPeers = new ArrayList<>();

    List<RaftServer> raftServers = new ArrayList<>();

    List<RaftNode> raftNodes = new ArrayList<>();

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
            RaftServer raftServer = new XRaftServer(raftServerOptions);
            raftServer.startup();

            raftServers.add(raftServer);
        }

        // create transport client
        TransportClient transportClient = createTransportClient();

        // every raft server start raft group
        for (int i = 0; i < peerNums; i++) {
            RaftServer raftServer = raftServers.get(i);
            RaftPeer raftPeer = raftPeers.get(i);

            RaftNodeOptions raftNodeOptions = new RaftNodeOptions();
            raftNodeOptions.setRaftPeer(raftPeer);
            raftNodeOptions.setShareTransportClientFlag(true);
            raftNodeOptions.setShareTransportClient(transportClient);
            raftNodeOptions.setInitialConf(new Configuration(raftPeers, new ArrayList<>()));
            raftNodeOptions.setRaftLogType(MemoryRaftLogType.memory);

            RaftGroupOptions raftGroupOptions = new RaftGroupOptions();
            raftGroupOptions.setRaftNodeOptions(raftNodeOptions);
            raftGroupOptions.setRaftGroupId(raftGroupId);

            RaftGroup raftGroup = raftServer.startRaftGroup(raftGroupOptions);

            raftNodes.add(raftGroup.getRaftNode());
        }

    }

    private TransportClient createTransportClient() {
        TransportClient transportClient = transportType.newClient(new TransportClientOptions());
        if (transportType == LocalTransportType.local) {
            LocalTransportClient localTransportClient = (LocalTransportClient) transportClient;
            localTransportClient.setRaftNodes(raftNodes);
        }

        return transportClient;
    }

    public RaftPeer getLeaderPeer() {
        for (RaftNode raftNode : raftNodes) {
            if (raftNode.getRaftRole() == RaftRole.LEADER) {
                return raftNode.getRaftPeer();
            }
        }
        return null;
    }

    public String printRaftNodes() {
        StringBuilder sb = new StringBuilder();
        for (RaftNode raftNode : raftNodes) {
            sb.append(raftNode.getRaftPeer()).append(":").append(raftNode.getRaftRole()).append(System.lineSeparator());
        }
        return sb.toString();
    }

}
