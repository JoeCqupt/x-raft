package io.github.xinfra.lab.raft.core.state;

import io.github.xinfra.lab.raft.RaftRole;
import io.github.xinfra.lab.raft.core.XRaftNode;
import io.github.xinfra.lab.raft.core.annotation.GuardByWriteLock;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class FollowerState {

    private final XRaftNode xRaftNode;

    private volatile Long lastLeaderRpcTimeMills = System.currentTimeMillis();

    private volatile boolean running;

    private Thread electionTimeoutTask;

    public FollowerState(XRaftNode xRaftNode) {
        this.xRaftNode = xRaftNode;
    }

    // guard by state write lock
    // todo: remove to raft node state
    @GuardByWriteLock
    public void updateLastRpcTimeMills(Long lastRpcTimeMills) {
        this.lastLeaderRpcTimeMills = lastRpcTimeMills;
    }

    @GuardByWriteLock
    public void startup() {
        if (running) {
            return;
        }
        running = true;
        electionTimeoutTask = new ElectionTimeoutTask();
        electionTimeoutTask.start();
    }

    @GuardByWriteLock
    public void shutdown() {
        if (!running) {
            return;
        }
        running = false;
        electionTimeoutTask.interrupt();
        electionTimeoutTask = null; // help gc
    }

    class ElectionTimeoutTask extends Thread {

        public ElectionTimeoutTask() {
            super("ElectionTimeoutTask");
        }

        @Override
        public void run() {
            while (shouldRun()) {
                try {
                    Long electionTimeoutMills = xRaftNode.getRaftNodeOptions().getRandomElectionTimeoutMills();
                    TimeUnit.MILLISECONDS.sleep(electionTimeoutMills);
                    try {
                        xRaftNode.getState().getWriteLock().lock();
                        if (xRaftNode.getState().getRole() != RaftRole.FOLLOWER) {
                            break;
                        }
                        if (timeout(electionTimeoutMills)) {
                            // start pre-vote
                            xRaftNode.getState().resetLeaderId(null);
                            preVote();
                        }
                    } finally {
                        xRaftNode.getState().getWriteLock().unlock();
                    }
                } catch (InterruptedException e) {
                    log.info("ElectionTimeoutTask thread interrupted");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Throwable t) {
                    log.error("ElectionTimeoutTask thread  ex", t);
                }
            }
        }


        public boolean timeout(long electionTimeoutMills) {
            return System.currentTimeMillis() - lastLeaderRpcTimeMills >= electionTimeoutMills;
        }

        private boolean shouldRun() {
            return running
                    && !Thread.currentThread().isInterrupted();
        }

    }

    private void preVote() {

    }

}
