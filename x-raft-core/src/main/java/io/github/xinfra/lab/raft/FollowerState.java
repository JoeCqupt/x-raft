package io.github.xinfra.lab.raft;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class FollowerState extends Thread {

    private final RaftServer raftServer;
    private Long lastRpcTime = System.currentTimeMillis();
    private volatile boolean running = true;

    public FollowerState(RaftServer raftServer) {
        this.raftServer = raftServer;
    }

    @Override
    public void run() {
        while (shouldRun()) {
            try {
                Long electionTimeout = raftServer.getRandomElectionTimeout();
                TimeUnit.MILLISECONDS.sleep(electionTimeout);
                if (!shouldRun()) {
                    break;
                }
                synchronized (raftServer) {
                    if (shouldRun() && timeout(electionTimeout)) {
                     raftServer.changeToCandidate();
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

    private boolean timeout(Long electionTimeout) {
        return System.currentTimeMillis() - lastRpcTime > electionTimeout;
    }

    public boolean shouldRun() {
        return running && raftServer.getState().getRole() == RaftRole.FOLLOWER;
    }
}
