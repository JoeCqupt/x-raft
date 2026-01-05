package io.github.xinfra.lab.raft.core.state;

import io.github.xinfra.lab.raft.RaftPeer;
import io.github.xinfra.lab.raft.RaftRole;
import io.github.xinfra.lab.raft.conf.ConfigurationEntry;
import io.github.xinfra.lab.raft.core.XRaftNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class LeaderState  {

	private static final Logger log = LoggerFactory.getLogger(LeaderState.class);

	private volatile boolean running;

	private final XRaftNode xRaftNode;

	private final LogReplicatorGroup logReplicatorGroup;

	private BlockingQueue<StateEvent> eventQueue = new ArrayBlockingQueue<>(4096);

	private Thread stateEventExecutor;

	public LeaderState(XRaftNode xRaftNode) {
		this.xRaftNode = xRaftNode;
		this.logReplicatorGroup = new LogReplicatorGroup(xRaftNode);
	}

	public  void startup() {
		if (running) {
			return;
		}
		running = true;

		// set leader id to getRaftPeer id
		xRaftNode.getState().resetLeaderId(xRaftNode.getRaftPeer().getRaftPeerId());
		// append   configurationEntry when leader startup
		ConfigurationEntry configurationEntry = xRaftNode.getState().getConfigState().getCurrentConfig();
		xRaftNode.getState().getRaftLog().append(configurationEntry);

		// init log appenders
		List<RaftPeer> raftPeers = configurationEntry.getPeers();
		for (RaftPeer raftPeer : raftPeers) {
			if (Objects.equals(raftPeer.getRaftPeerId() , xRaftNode.getRaftPeerId())){
				continue;
			}
			logReplicatorGroup.addLogReplicator(raftPeer);
		}
		List<RaftPeer> learners = configurationEntry.getLearners();
		for (RaftPeer raftPeer : learners) {
			if (Objects.equals(raftPeer.getRaftPeerId() , xRaftNode.getRaftPeerId())){
				continue;
			}
			logReplicatorGroup.addLogReplicator(raftPeer);
		}

		stateEventExecutor = new StateEventExecutor();
		stateEventExecutor.start();
	}

	public  void shutdown() {
		if (!running) {
			return;
		}
		running = false;
		// todo: shutdown log appenders and etc things
		for (LogReplicator logReplicator : logReplicators) {
			logReplicator.shutdown();
		}
		logReplicators.clear();
		stateEventExecutor.interrupt();
		stateEventExecutor = null;
	}

	class StateEventExecutor extends Thread {

		public StateEventExecutor() {
			super("StateEventExecutor");
		}

		@Override
		public void run() {
			while (shouldRun()) {
				try {
					// todo: poll timeout config
					StateEvent event = eventQueue.poll(100, TimeUnit.MILLISECONDS);
					// todo: write lock
							if (event != null) {
								event.execute();
							}
							// todo: leader other logic

				}
				catch (InterruptedException ie) {
					log.warn("StateEventExecutor thread is interrupted");
					break;
				}
				catch (Exception e) {
					log.warn("StateEventExecutor thread ex", e);
				}
			}
		}

		private boolean shouldRun() {
			return running && !Thread.currentThread().isInterrupted();
		}

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
