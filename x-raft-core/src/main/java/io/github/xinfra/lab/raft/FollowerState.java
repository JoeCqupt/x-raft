package io.github.xinfra.lab.raft;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class FollowerState extends Thread {

    private final XRaftNode xRaftNode;
    private Long lastRpcTime = System.currentTimeMillis();
    private volatile boolean running = true;

    public FollowerState(XRaftNode xRaftNode) {
        this.xRaftNode = xRaftNode;
    }

    @Override
    public void run() {
        while (shouldRun()) {
            try {
                Long electionTimeout = xRaftNode.getRandomElectionTimeout();
                TimeUnit.MILLISECONDS.sleep(electionTimeout);
                synchronized (xRaftNode) {
                    if (shouldRun() && timeout(electionTimeout)) {
                        xRaftNode.changeToCandidate();
                    }
                }
            } catch (InterruptedException e) {
                log.info("FollowerState interrupted");
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable t) {
                log.error("FollowerState error", t);
            }
        }
    }

    public void updateLastRpcTime(Long lastRpcTime) {
        this.lastRpcTime = lastRpcTime;
    }

    private boolean timeout(Long electionTimeout) {
        return System.currentTimeMillis() - lastRpcTime > electionTimeout;
    }

    public boolean shouldRun() {
        return running && xRaftNode.getState().getRole() == RaftRole.FOLLOWER;
    }

    public void shutdown() {
        running = false;
    }
}
