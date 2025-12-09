package io.github.xinfra.lab.raft.core.state;

import io.github.xinfra.lab.raft.RaftRole;
import io.github.xinfra.lab.raft.core.XRaftNode;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class FollowerState {

	private final XRaftNode xRaftNode;

	private volatile Long lastRpcTimeMills = System.currentTimeMillis();

	private volatile boolean running = true;

	private Thread electionTimeoutTask ;

	public FollowerState(XRaftNode xRaftNode) {
		this.xRaftNode = xRaftNode;
	}

	public void updateLastRpcTimeMills(Long lastRpcTimeMills) {
		this.lastRpcTimeMills = lastRpcTimeMills;
	}

	public void startup() {
		electionTimeoutTask = new ElectionTimeoutTask();
		electionTimeoutTask.start();
	}

	public void shutdown() {
		running = false;
		if (electionTimeoutTask != null) {
			electionTimeoutTask.interrupt();
			electionTimeoutTask = null; // help gc
		}
	}

	 class ElectionTimeoutTask extends Thread {
		 public ElectionTimeoutTask() {
			 super("ElectionTimeoutTask");
		 }

		 @Override
		public void run() {
			while (shouldRun()) {
				try {
					Long electionTimeoutMills = xRaftNode.getRandomElectionTimeoutMills();
					TimeUnit.MILLISECONDS.sleep(electionTimeoutMills);
					synchronized (xRaftNode) {
						if (shouldRun() && timeout(electionTimeoutMills)) {
							xRaftNode.getState().changeToCandidate();
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

		 private boolean timeout(Long electionTimeout) {
			 return System.currentTimeMillis() - lastRpcTimeMills >= electionTimeout;
		 }

		 private boolean shouldRun() {
			 return running && xRaftNode.getState().getRole() == RaftRole.FOLLOWER;
		 }
	}

}
