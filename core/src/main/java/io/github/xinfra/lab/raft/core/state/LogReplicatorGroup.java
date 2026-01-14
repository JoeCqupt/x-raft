package io.github.xinfra.lab.raft.core.state;

import io.github.xinfra.lab.raft.RaftPeer;
import io.github.xinfra.lab.raft.RaftRole;
import io.github.xinfra.lab.raft.core.XRaftNode;
import io.github.xinfra.lab.raft.protocol.AppendEntriesRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class LogReplicatorGroup {

    private final XRaftNode xRaftNode;

    Map<String, LogReplicator> logReplicators = new ConcurrentHashMap<>();

    public LogReplicatorGroup(XRaftNode xRaftNode) {
        this.xRaftNode = xRaftNode;
    }

    public void addLogReplicator(RaftPeer raftPeer) {
        if (logReplicators.containsKey(raftPeer.getRaftPeerId())) {
            return;
        }
        // todo: nextIdx
        LogReplicator logReplicator = new LogReplicator(raftPeer);
        logReplicators.put(raftPeer.getRaftPeerId(), logReplicator);
        logReplicator.start();
    }

    public void shutdown() {
        for (LogReplicator logReplicator : logReplicators.values()) {
            logReplicator.shutdown();
        }
    }

    @Data
    class LogReplicator extends Thread {

        private volatile boolean running = true;

        private final RaftPeer raftPeer;

        private Long nextIndex;

        private Long matchIndex = -1L;

        private Long lastAppendSendTime;

        public LogReplicator(RaftPeer raftPeer) {
            this.raftPeer = raftPeer;
        }

        @Override
        public void run() {
            while (shouldRun()) {
                if (shouldAppend()) {
                    sendAppendEntries();
                }
            }
        }

        private void sendAppendEntries() {
            // todo

            AppendEntriesRequest request = new AppendEntriesRequest();
            request.setLeaderId(xRaftNode.getState().getLeaderId().get());

        }

        private boolean shouldAppend() {
            return hasEntries() || heartbeatLeftTimeMills() <= 0;
        }

        private Long heartbeatLeftTimeMills() {
            if (lastAppendSendTime == 0) {
                // run first time
                return 0L;
            }
            // todo: to calculate it
            // todo: why
            Long electionTimeoutMills = xRaftNode.getRaftNodeOptions().getElectionTimeoutMills();
            Long noHeartbeatTimeMills = System.currentTimeMillis() - lastAppendSendTime;
            return (electionTimeoutMills / 3) - noHeartbeatTimeMills;
        }

        private boolean hasEntries() {
            return nextIndex < xRaftNode.raftLog().getNextIndex();
        }

        private boolean shouldRun() {
            return running && xRaftNode.getState().getRole() == RaftRole.LEADER;
        }

        public void shutdown() {
            running = false;
            this.interrupt();
        }

    }

}