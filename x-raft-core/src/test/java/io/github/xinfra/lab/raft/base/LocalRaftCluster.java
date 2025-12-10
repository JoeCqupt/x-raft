package io.github.xinfra.lab.raft.base;

import io.github.xinfra.lab.raft.RaftGroup;
import io.github.xinfra.lab.raft.RaftPeer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class LocalRaftCluster {

    private String raftGroupId;
    List<LocalXRaftNode> raftNodes ;
    List<RaftPeer> raftPeers;
    private RaftGroup raftGroup;
    private int nodeNums;

    private static int initPort = 5000;

    public LocalRaftCluster(String raftGroupId, int nodeNums) {
        this.raftGroupId = raftGroupId;
        this.nodeNums = nodeNums;
    }


    public void startup() {
        startupNodes();
    }

    private void startupNodes() {
        raftPeers = new ArrayList<>();
        for (int i = 0; i < nodeNums; i++) {
            RaftPeer raftPeer = new RaftPeer();
            raftPeer.setRaftPeerId("node" + i);
            raftPeer.setAddress(new InetSocketAddress("localhost", 5000 + i));
            raftPeers.add(raftPeer);
        }
        raftGroup = new RaftGroup(raftGroupId, raftPeers);
    }
}
