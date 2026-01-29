package io.github.xinfra.lab.raft.core.state;

import io.github.xinfra.lab.raft.RaftPeer;
import io.github.xinfra.lab.raft.RaftRole;
import io.github.xinfra.lab.raft.core.XRaftNode;
import io.github.xinfra.lab.raft.core.transport.RaftApi;
import io.github.xinfra.lab.raft.log.LogEntry;
import io.github.xinfra.lab.raft.log.RaftLog;
import io.github.xinfra.lab.raft.protocol.AppendEntriesRequest;
import io.github.xinfra.lab.raft.protocol.AppendEntriesResponse;
import io.github.xinfra.lab.raft.transport.CallOptions;
import io.github.xinfra.lab.raft.transport.ResponseCallBack;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
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
		Long nextIndex = xRaftNode.getState().getRaftLog().getNextIndex();
		Long term = xRaftNode.getState().getCurrentTerm();
		String leaderId = xRaftNode.getState().getLeaderId();

		LogReplicator logReplicator = new LogReplicator(raftPeer, nextIndex, term, leaderId);
		logReplicators.put(raftPeer.getRaftPeerId(), logReplicator);
		logReplicator.start();
	}

	public void shutdown() {
		for (LogReplicator logReplicator : logReplicators.values()) {
			logReplicator.shutdown();
		}
	}

	/**
	 * TODO: 流水线复制 优化
	 */
	class LogReplicator extends Thread {

		private volatile boolean running = true;

		private final RaftPeer raftPeer;

		private Long nextIndex;

		private Long term;

		private String leaderId;

		private Long matchIndex = -1L;

		private Long lastAppendSendTime;

		public LogReplicator(RaftPeer raftPeer, Long nextIndex, Long term, String leaderId) {
			this.raftPeer = raftPeer;
			this.nextIndex = nextIndex;
			this.term = term;
			this.leaderId = leaderId;
		}

		@Override
		public void run() {
			while (shouldRun()) {
				try {
					if (shouldAppend()) {
						appendEntries();
					}
				}
				catch (Exception e) {
					log.error("LogReplicator error", e);
				}
			}
		}

		private void appendEntries() {
			AppendEntriesRequest request = new AppendEntriesRequest();
			request.setRaftGroupId(xRaftNode.getRaftGroupId());
			request.setRaftPeerId(raftPeer.getRaftPeerId());
			request.setTerm(term);
			request.setLeaderId(leaderId);
			request.setLeaderCommit(xRaftNode.getState().getCommitIndex());

			// 设置 prevLogIndex 和 prevLogTerm
			Long prevLogIndex = nextIndex - 1;
			Long prevLogTerm = 0L;
			if (prevLogIndex >= 0) {
				LogEntry prevEntry = xRaftNode.getState().getRaftLog().getEntry(prevLogIndex);
				if (prevEntry != null) {
					prevLogTerm = prevEntry.term();
				}
			}
			request.setPrevLogIndex(prevLogIndex);
			request.setPrevLogTerm(prevLogTerm);

			// 设置日志条目
			List<LogEntry> entries;
			if (hasMoreEntries()) {
				// 有新日志需要复制
				Long endIndex = xRaftNode.getState().getRaftLog().getNextIndex();
				entries = xRaftNode.getState().getRaftLog().getEntries(nextIndex, endIndex);
			}
			else {
				// 心跳消息，不携带日志条目
				entries = Collections.emptyList();
			}
			request.setEntries(entries);

			// 发送请求
			CallOptions callOptions = new CallOptions();
			callOptions.setTimeoutMs(xRaftNode.getRaftNodeOptions().getElectionTimeoutMills());

			try {
				xRaftNode.getTransportClient()
					.asyncCall(RaftApi.appendEntries, request, raftPeer.getAddress(), callOptions,
							new AppendEntriesResponseCallBack(term, raftPeer,
									request.getPrevLogIndex() + entries.size()));
			}
			catch (Exception e) {
				log.error("Failed to send appendEntries to peer: {}", raftPeer.getRaftPeerId(), e);
			}
			finally {
				lastAppendSendTime = System.currentTimeMillis();
			}
		}

		private boolean shouldAppend() {
			return xRaftNode.getState().getRole() == RaftRole.LEADER && (hasMoreEntries() || timeoutHeartbeat());
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

		/**
		 * 在 leader 的日志中查找指定 term 的最后一个索引
		 * @param term 要查找的 term
		 * @return 该 term 的最后一个索引，如果不存在则返回 null
		 */
		private Long findLastIndexOfTerm(long term) {
			RaftLog raftLog = xRaftNode.getState().getRaftLog();
			long lastIndex = raftLog.getLastEntryTermIndex().getIndex();

			// 从后向前查找
			for (long i = lastIndex; i >= 0; i--) {
				LogEntry entry = raftLog.getEntry(i);
				if (entry != null && entry.term() == term) {
					return i;
				}
				// 如果遇到更小的 term，说明不存在该 term
				if (entry != null && entry.term() < term) {
					break;
				}
			}
			return null;
		}

		public void shutdown() {
			running = false;
			this.interrupt();
		}

		/**
		 * AppendEntries 响应回调处理
		 */
		class AppendEntriesResponseCallBack implements ResponseCallBack<AppendEntriesResponse> {

			long term;

			private final RaftPeer raftPeer;

			Long expectMatchIndex;

			public AppendEntriesResponseCallBack(long term, RaftPeer raftPeer, Long expectMatchIndex) {
				this.term = term;
				this.raftPeer = raftPeer;
				this.expectMatchIndex = expectMatchIndex;
			}

			@Override
			public void onResponse(AppendEntriesResponse response) {
				try {
					// 检查当前角色是否还是 Leader
					if (xRaftNode.getState().getRole() != RaftRole.LEADER) {
						log.warn(
								"AppendEntriesResponseCallBack: current role is not LEADER, ignore response from peer:{}",
								raftPeer.getRaftPeerId());
						return;
					}

					// 检查 term 是否匹配
					if (term != xRaftNode.getState().getCurrentTerm()) {
						log.warn(
								"AppendEntriesResponseCallBack: term mismatch, request term:{}, current term:{}, ignore response from peer:{}",
								term, xRaftNode.getState().getCurrentTerm(), raftPeer.getRaftPeerId());
						return;
					}

					// 如果响应的 term 更大，说明当前节点已经过期，转为 Follower
					if (response.getTerm() > xRaftNode.getState().getCurrentTerm()) {
						log.warn(
								"AppendEntriesResponseCallBack: received higher term:{} from peer:{}, current term:{}, change to follower",
								response.getTerm(), raftPeer.getRaftPeerId(), xRaftNode.getState().getCurrentTerm());
						xRaftNode.getState().changeToFollower(response.getTerm());
						return;
					}

					// 处理成功响应
					if (response.isSuccess()) {
						matchIndex = expectMatchIndex;
						nextIndex = matchIndex + 1;
						log.debug("AppendEntries success to peer:{}, matchIndex:{}, nextIndex:{}",
								raftPeer.getRaftPeerId(), matchIndex, nextIndex);
					}
					else {
						// 处理失败响应：使用 conflictIndex 和 conflictTerm 优化日志回退
						Long oldNextIndex = nextIndex;
						if (response.getConflictIndex() != null && response.getConflictTerm() != null) {
							long conflictIndex = response.getConflictIndex();
							long conflictTerm = response.getConflictTerm();

							log.debug("AppendEntries failed to peer:{}, conflictIndex:{}, conflictTerm:{}",
									raftPeer.getRaftPeerId(), conflictIndex, conflictTerm);

							if (conflictTerm == 0) {
								// Follower 的日志比 prevLogIndex 短，直接使用 conflictIndex
								nextIndex = conflictIndex;
							}
							else {
								// Follower 在 prevLogIndex 位置有不同的 term
								// 在 leader 的日志中查找 conflictTerm 的最后一个索引
								Long leaderConflictIndex = findLastIndexOfTerm(conflictTerm);

								if (leaderConflictIndex != null) {
									// Leader 也有该 term 的日志，从该 term 的下一个位置开始
									nextIndex = leaderConflictIndex + 1;
								}
								else {
									// Leader 没有该 term 的日志，使用 follower 提供的 conflictIndex
									nextIndex = conflictIndex;
								}
							}

							log.info("AppendEntries failed to peer:{}, adjusted nextIndex from {} to {}",
									raftPeer.getRaftPeerId(), oldNextIndex, nextIndex);
						}
						else {
							// 没有冲突信息，回退一个位置（传统方式）
							nextIndex = Math.max(0, nextIndex - 1);
							log.info("AppendEntries failed to peer:{}, no conflict info, decrement nextIndex to {}",
									raftPeer.getRaftPeerId(), nextIndex);
						}
					}
				}
				catch (Exception e) {
					log.error("AppendEntriesResponseCallBack error", e);
				}
			}

			@Override
			public void onException(Throwable throwable) {
				log.error("AppendEntries to peer:{} exception", raftPeer.getRaftPeerId(), throwable);
			}

		}

	}

}
