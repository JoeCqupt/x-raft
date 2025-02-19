package io.github.xinfra.lab.raft.core.state;

import io.github.xinfra.lab.raft.RaftPeer;
import io.github.xinfra.lab.raft.RaftRole;
import io.github.xinfra.lab.raft.core.XRaftNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class LeaderState extends Thread {

	private static final Logger log = LoggerFactory.getLogger(LeaderState.class);

	private volatile boolean running = true;

	private final XRaftNode xRaftNode;

	private List<LogReplicator> logReplicators = new ArrayList<>();

	private BlockingQueue<StateEvent> eventQueue = new ArrayBlockingQueue<>(4096);

	public LeaderState(XRaftNode xRaftNode) {
		this.xRaftNode = xRaftNode;
		leaderStartup();
	}

	@Override
	public void run() {
		while (shouldRun()) {
			try {
				// todo: poll timeout config
				StateEvent event = eventQueue.poll(100, TimeUnit.MILLISECONDS);
				synchronized (xRaftNode) {
					if (shouldRun()) {
						if (event != null) {
							event.execute();
						}
						// todo: leader other logic
					}
				}
			}
			catch (InterruptedException ie) {
				log.warn("LeaderState thread is interrupted");
				break;
			}
			catch (Exception e) {
				log.warn("LeaderState thread ex", e);
			}
		}
	}

	private boolean shouldRun() {
		return running && xRaftNode.getState().getRole() == RaftRole.LEADER;
	}

	private void leaderStartup() {
		// set leader id to self id
		xRaftNode.getState().getLeaderId().set(xRaftNode.self().getRaftPeerId());
		// append an entry to log when leader startup
		// todo: append a no-op entry or configuration entry
		xRaftNode.raftLog().append(null);

		// init log appenders
		Set<RaftPeer> otherRaftPeers = xRaftNode.getState().getRaftConfiguration().getOtherRaftPeers();
		for (RaftPeer raftPeer : otherRaftPeers) {
			logReplicators.add(new LogReplicator(raftPeer, xRaftNode));
		}
		// start log appenders
		for (LogReplicator logReplicator : logReplicators) {
			logReplicator.start();
		}
	}

	public void shutdown() {
		running = false;
		// todo: shutdown log appenders and etc things
	}

	static class StateEvent {

		Runnable runnable;

		Long term;

		EventType eventType;

		public void execute() {
			runnable.run();
		}

	}

	static enum EventType {

	}

}
