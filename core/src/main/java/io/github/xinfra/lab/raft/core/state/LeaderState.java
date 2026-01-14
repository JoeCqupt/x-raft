package io.github.xinfra.lab.raft.core.state;

import io.github.xinfra.lab.raft.RaftPeer;
import io.github.xinfra.lab.raft.conf.ConfigurationEntry;
import io.github.xinfra.lab.raft.core.XRaftNode;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

@Slf4j
public class LeaderState {

    private volatile boolean running;

    private final XRaftNode xRaftNode;

    private LogReplicatorGroup logReplicatorGroup;


    public LeaderState(XRaftNode xRaftNode) {
        this.xRaftNode = xRaftNode;
    }

    public void startup() {
        if (running) {
            return;
        }
        running = true;
        this.logReplicatorGroup = new LogReplicatorGroup(xRaftNode);

        // set leader id to getRaftPeer id
        xRaftNode.getState().resetLeaderId(xRaftNode.getRaftPeer().getRaftPeerId());
        // append configurationEntry when leader startup
        ConfigurationEntry configurationEntry = xRaftNode.getState().getConfigState().getCurrentConfig();
        xRaftNode.getState().getRaftLog().append(configurationEntry);

        // add log replicator
        List<RaftPeer> raftPeers = configurationEntry.getPeers();
        for (RaftPeer raftPeer : raftPeers) {
            if (Objects.equals(raftPeer.getRaftPeerId(), xRaftNode.getRaftPeerId())) {
                continue;
            }
            logReplicatorGroup.addLogReplicator(raftPeer);
        }
        List<RaftPeer> learners = configurationEntry.getLearners();
        for (RaftPeer raftPeer : learners) {
            if (Objects.equals(raftPeer.getRaftPeerId(), xRaftNode.getRaftPeerId())) {
                continue;
            }
            logReplicatorGroup.addLogReplicator(raftPeer);
        }

    }

    public void shutdown() {
        if (!running) {
            return;
        }
        running = false;
        logReplicatorGroup.shutdown();
        logReplicatorGroup = null;
    }

}
