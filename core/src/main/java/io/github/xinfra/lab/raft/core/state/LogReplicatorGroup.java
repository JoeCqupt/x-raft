package io.github.xinfra.lab.raft.core.state;

import io.github.xinfra.lab.raft.RaftPeer;
import io.github.xinfra.lab.raft.RaftRole;
import io.github.xinfra.lab.raft.core.XRaftNode;
import io.github.xinfra.lab.raft.protocol.AppendEntriesRequest;
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
        LogReplicator logReplicator = new LogReplicator(raftPeer, xRaftNode.getState().getRaftLog().getNextIndex());
        logReplicators.put(raftPeer.getRaftPeerId(), logReplicator);
        logReplicator.start();
    }

    public void shutdown() {
        for (LogReplicator logReplicator : logReplicators.values()) {
            logReplicator.shutdown();
        }
    }

    /**
     * 流水线复制 优化
     */
    @Slf4j
    class LogReplicator extends Thread {

        private volatile boolean running = true;

        private final RaftPeer raftPeer;

        private Long nextIndex;

        private Long matchIndex = -1L; // todo: no use

        private Long lastAppendSendTime; // todo: no use

        public LogReplicator(RaftPeer raftPeer, Long nextIndex) {
            this.raftPeer = raftPeer;
            this.nextIndex = nextIndex;
        }

        @Override
        public void run() {
            while (shouldRun()) {
                if (shouldAppend()) {
                    appendEntries();
                }
            }
        }

        private void appendEntries() {
            if (timeoutHeartbeat()){
                AppendEntriesRequest request = new AppendEntriesRequest();
                request.setTerm(xRaftNode.getState().getCurrentTerm());
                request.setLeaderId(xRaftNode.getState().getLeaderId());

            } else {

            }

        }

        private boolean shouldAppend() {
            return xRaftNode.getState().getRole() == RaftRole.LEADER &&
                    (hasMoreEntries() || timeoutHeartbeat());
        }

        private boolean timeoutHeartbeat() {
            if (lastAppendSendTime == 0) {
                // run first time
                return true;
            }
            // todo: to calculate it
            // todo: why
            Long electionTimeoutMills = xRaftNode.getRaftNodeOptions().getElectionTimeoutMills();
            Long noHeartbeatTimeMills = System.currentTimeMillis() - lastAppendSendTime;
            return ((electionTimeoutMills / 3) - noHeartbeatTimeMills) <= 0L;
        }

        private boolean hasMoreEntries() {
            return nextIndex < xRaftNode.getState().getRaftLog().getNextIndex();
        }

        private boolean shouldRun() {
            return running && !Thread.currentThread().isInterrupted();
        }

        public void shutdown() {
            running = false;
            this.interrupt();
        }

    }

}