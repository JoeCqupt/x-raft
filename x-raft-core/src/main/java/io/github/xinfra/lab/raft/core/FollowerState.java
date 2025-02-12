package io.github.xinfra.lab.raft.core;

import io.github.xinfra.lab.raft.RaftRole;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class FollowerState extends Thread {

	private final XRaftNode xRaftNode;

	private volatile Long lastRpcTimeMills = System.currentTimeMillis();

	private volatile boolean running = true;

	public FollowerState(XRaftNode xRaftNode) {
		this.xRaftNode = xRaftNode;
	}

	@Override
	public void run() {
		while (shouldRun()) {
			try {
				long electionTimeoutMills = xRaftNode.getRandomElectionTimeoutMills();
				TimeUnit.MILLISECONDS.sleep(electionTimeoutMills);
				synchronized (xRaftNode) {
					if (shouldRun() && timeout(electionTimeoutMills)) {
						xRaftNode.changeToCandidate();
						break;
					}
				}
			}
			catch (InterruptedException e) {
				log.info("FollowerState interrupted");
				Thread.currentThread().interrupt();
				break;
			}
			catch (Throwable t) {
				log.error("FollowerState error", t);
			}
		}
	}

	public void updateLastRpcTimeMills(Long lastRpcTimeMills) {
		this.lastRpcTimeMills = lastRpcTimeMills;
	}

	private boolean timeout(long electionTimeout) {
		return System.currentTimeMillis() - lastRpcTimeMills >= electionTimeout;
	}

	public boolean shouldRun() {
		return running && xRaftNode.getState().getRole() == RaftRole.FOLLOWER;
	}

	public void shutdown() {
		running = false;
	}

}
