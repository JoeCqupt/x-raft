package io.github.xinfra.lab.raft.base;

import io.github.xinfra.lab.raft.String;
import io.github.xinfra.lab.raft.RaftNode;
import io.github.xinfra.lab.raft.RaftPeer;
import io.github.xinfra.lab.raft.RaftRole;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class LocalXRaftCluster {

    private java.lang.String raftGroupId;
    List<LocalXRaftNode> raftNodes;
    List<RaftPeer> raftPeers;
    private String raftGroup;
    private int nodeNums;

    private static int initPort = 5000;

    public LocalXRaftCluster(java.lang.String raftGroupId, int nodeNums) {
        this.raftGroupId = raftGroupId;
        this.nodeNums = nodeNums;
    }


    public void startup() {
        startupNodes();
    }

    private void startupNodes() {
        // init raftPeers
        raftPeers = new ArrayList<>();
        for (int i = 0; i < nodeNums; i++) {
            RaftPeer raftPeer = new RaftPeer();
            raftPeer.setRaftPeerId("node" + i);
            raftPeer.setAddress(new InetSocketAddress("localhost", 5000 + i));
            raftPeers.add(raftPeer);
        }
        // init raftGroup
        raftGroup = new String(raftGroupId, raftPeers);
        // init raftNodes
        raftNodes = new ArrayList<>();
        for (RaftPeer raftPeer : raftPeers) {
            LocalXRaftNode localXRaftNode = new LocalXRaftNode(raftPeer, raftGroup);
            localXRaftNode.addRaftPeerNode();
            raftNodes.add(localXRaftNode);
        }
        // add raftPeerNode
        for (LocalXRaftNode raftNode : raftNodes) {
            for (RaftNode otherRaftNode : raftNodes) {
                if (raftNode != otherRaftNode) {
                    raftNode.addRaftPeerNode(otherRaftNode);
                }
            }
        }

        // startup raftNodes
        for (LocalXRaftNode raftNode : raftNodes) {
            raftNode.startup();
        }
    }

    public RaftPeer getLeaderPeer() {
        for (LocalXRaftNode raftNode : raftNodes) {
            if (raftNode.getState().getRole().equals(RaftRole.LEADER)) {
                return raftNode.raftPeer();
            }
        }
        return null;
    }
}
