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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class LogReplicatorGroup {

	private final XRaftNode xRaftNode;

	Map<String, LogReplicator> logReplicators = new ConcurrentHashMap<>();

	/**
	 * 日志变化通知对象 当 leader 的日志发生变化时，通过此对象通知所有 LogReplicator 进行日志复制
	 */
	private final Object logChangeNotifier = new Object();

	public LogReplicatorGroup(XRaftNode xRaftNode) {
		this.xRaftNode = xRaftNode;
	}

	/**
	 * 通知所有 LogReplicator 日志已变化，需要进行复制 此方法应该在 leader 添加新日志后调用
	 */
	public void notifyLogChanged() {
		synchronized (logChangeNotifier) {
			logChangeNotifier.notifyAll();
			log.debug("Notified all LogReplicators about log change");
		}
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
	 * 使用流水线复制优化日志复制流程 1. 保障复制的顺序性：使用 inflightRequests 队列维护正在进行的请求 2.
	 * 保障复制失败后从失败点位重新复制：记录失败的 nextIndex，从该位置重新开始
	 */
	class LogReplicator extends Thread {

		private volatile boolean running = true;

		private final RaftPeer raftPeer;

		/**
		 * 下一个要复制的日志索引（已确认的位置）
		 */
		private Long nextIndex;

		/**
		 * 下一个要发送的日志索引（已发送但未确认的位置）
		 */
		private Long nextSendIndex;

		private Long term;

		private String leaderId;

		private Long matchIndex = 0L;

		private Long lastAppendSendTime;

		/**
		 * 流水线中正在进行的请求队列 用于保障复制的顺序性
		 */
		private final LinkedBlockingQueue<InflightRequest> inflightRequests = new LinkedBlockingQueue<>();

		/**
		 * 缓存乱序到达的响应，等待按序处理 Key: sequence, Value: response
		 */
		private final Map<Long, AppendEntriesResponse> pendingResponses = new ConcurrentHashMap<>();

		/**
		 * 流水线中允许的最大并发请求数
		 */
		private static final int MAX_INFLIGHT_REQUESTS = 256;

		/**
		 * 下一个要发送的序列号
		 */
		private final AtomicLong nextSequence = new AtomicLong(0);

		/**
		 * 下一个要处理响应的序列号
		 */
		private final AtomicLong nextResponseSequence = new AtomicLong(0);

		/**
		 * 是否正在进行探测（日志不匹配时的回退过程）
		 */
		private volatile boolean probing = false;

		public LogReplicator(RaftPeer raftPeer, Long nextIndex, Long term, String leaderId) {
			this.raftPeer = raftPeer;
			this.nextIndex = nextIndex;
			this.nextSendIndex = nextIndex;
			this.term = term;
			this.leaderId = leaderId;
		}

		@Override
		public void run() {
			while (shouldRun()) {
				try {
					if (shouldAppend()) {
						// 流水线模式：允许多个请求并发进行
						if (canSendMoreRequests()) {
							appendEntries();
						}
						else {
							// 流水线已满，等待一段时间后重试
							Thread.sleep(10);
						}
					}
					else {
						// 没有需要发送的请求，等待日志变化通知
						synchronized (logChangeNotifier) {
							// 双重检查：在获取锁后再次检查是否有日志需要发送
							// 避免在等待期间错过通知
							if (!shouldAppend()) {
								// 等待通知，最多等待 100ms（作为保护机制，避免永久等待）
								logChangeNotifier.wait(100);
							}
						}
					}
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
				catch (Exception e) {
					log.error("LogReplicator error", e);
				}
			}
		}

		/**
		 * 检查是否可以发送更多请求 在探测模式下，只允许一个请求在进行 在正常模式下，允许多个请求并发
		 */
		private boolean canSendMoreRequests() {
			if (probing) {
				// 探测模式：只允许一个请求
				return inflightRequests.isEmpty();
			}
			else {
				// 正常模式：允许多个请求并发
				return inflightRequests.size() < MAX_INFLIGHT_REQUESTS;
			}
		}

		private void appendEntries() {
			AppendEntriesRequest request = new AppendEntriesRequest();
			request.setRaftGroupId(xRaftNode.getRaftGroupId());
			request.setRaftPeerId(raftPeer.getRaftPeerId());
			request.setTerm(term);
			request.setLeaderId(leaderId);
			request.setLeaderCommit(xRaftNode.getState().getCommitIndex());

			// 使用 nextSendIndex 作为发送起点
			// 设置 prevLogIndex 和 prevLogTerm
			Long prevLogIndex = nextSendIndex - 1;
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
			Long startIndex = nextSendIndex;
			Long endIndex;
			if (hasMoreEntriesToSend()) {
				// 有新日志需要复制
				endIndex = xRaftNode.getState().getRaftLog().getNextIndex();
				// 限制每次发送的日志条目数量，避免单个请求过大
				long maxEntries = 1024;
				if (endIndex - startIndex > maxEntries) {
					endIndex = startIndex + maxEntries;
				}
				entries = xRaftNode.getState().getRaftLog().getEntries(startIndex, endIndex);
			}
			else {
				// 心跳消息，不携带日志条目
				entries = Collections.emptyList();
				endIndex = startIndex;
			}
			request.setEntries(entries);

			// 创建流水线请求对象
			long sequence = nextSequence.getAndIncrement();
			InflightRequest inflightRequest = new InflightRequest(sequence, startIndex, endIndex);

			// 发送请求
			CallOptions callOptions = new CallOptions();
			callOptions.setTimeoutMs(xRaftNode.getRaftNodeOptions().getElectionTimeoutMills());

			try {
				// 将请求加入流水线队列
				inflightRequests.offer(inflightRequest);

				// 更新 nextSendIndex，为下一个请求做准备
				// 注意：这里只更新发送位置，不更新确认位置 nextIndex
				nextSendIndex = endIndex;

				xRaftNode.getTransportClient()
					.asyncCall(RaftApi.appendEntries, request, raftPeer.getAddress(), callOptions,
							new AppendEntriesResponseCallBack(term, raftPeer, inflightRequest));

				log.debug("Sent appendEntries to peer:{}, sequence:{}, startIndex:{}, endIndex:{}, entriesSize:{}",
						raftPeer.getRaftPeerId(), sequence, startIndex, endIndex, entries.size());
				lastAppendSendTime = System.currentTimeMillis();
			}
			catch (Exception e) {
				log.error("Failed to send appendEntries to peer: {}", raftPeer.getRaftPeerId(), e);
				// 发送失败，从队列中移除
				inflightRequests.remove(inflightRequest);
				// 恢复 nextSendIndex
				nextSendIndex = startIndex;
			}
		}

		private boolean shouldAppend() {
			return xRaftNode.getState().getRole() == RaftRole.LEADER && (hasMoreEntriesToSend() || timeoutHeartbeat());
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

		/**
		 * 检查是否有更多日志需要发送 使用 nextSendIndex 而不是 nextIndex
		 */
		private boolean hasMoreEntriesToSend() {
			return nextSendIndex < xRaftNode.getState().getRaftLog().getNextIndex();
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
		 * 获取当前的 matchIndex
		 */
		public Long getMatchIndex() {
			return matchIndex;
		}

		/**
		 * 流水线请求对象
		 */
		class InflightRequest {

			final long sequence;

			final Long startIndex;

			final Long endIndex;

			public InflightRequest(long sequence, Long startIndex, Long endIndex) {
				this.sequence = sequence;
				this.startIndex = startIndex;
				this.endIndex = endIndex;
			}

			/**
			 * 获取期望的 matchIndex（已复制的最高日志索引） matchIndex = endIndex - 1（因为 endIndex
			 * 是下一个要发送的位置）
			 */
			public Long getExpectMatchIndex() {
				return endIndex - 1;
			}

			/**
			 * 获取期望的 nextIndex（下一个要发送的日志索引） nextIndex = endIndex
			 */
			public Long getExpectNextIndex() {
				return endIndex;
			}

		}

		/**
		 * AppendEntries 响应回调处理 使用流水线机制保障顺序性和失败重试
		 */
		class AppendEntriesResponseCallBack implements ResponseCallBack<AppendEntriesResponse> {

			long term;

			private final RaftPeer raftPeer;

			private final InflightRequest inflightRequest;

			public AppendEntriesResponseCallBack(long term, RaftPeer raftPeer, InflightRequest inflightRequest) {
				this.term = term;
				this.raftPeer = raftPeer;
				this.inflightRequest = inflightRequest;
			}

			@Override
			public void onResponse(AppendEntriesResponse response) {
				try {
					// 检查当前角色是否还是 Leader
					if (xRaftNode.getState().getRole() != RaftRole.LEADER) {
						log.warn(
								"AppendEntriesResponseCallBack: current role is not LEADER, ignore response from peer:{}, sequence:{}",
								raftPeer.getRaftPeerId(), inflightRequest.sequence);
						// 从流水线队列中移除该请求
						inflightRequests.remove(inflightRequest);
						// 清空流水线队列和待处理响应
						clearInflightRequests();
						pendingResponses.clear();
						return;
					}

					// 检查 term 是否匹配
					if (term != xRaftNode.getState().getCurrentTerm()) {
						log.warn(
								"AppendEntriesResponseCallBack: term mismatch, request term:{}, current term:{}, ignore response from peer:{}, sequence:{}",
								term, xRaftNode.getState().getCurrentTerm(), raftPeer.getRaftPeerId(),
								inflightRequest.sequence);
						// 从流水线队列中移除该请求
						inflightRequests.remove(inflightRequest);
						// 清空流水线队列和待处理响应
						clearInflightRequests();
						pendingResponses.clear();
						return;
					}

					// 如果响应的 term 更大，说明当前节点已经过期，转为 Follower
					if (response.getTerm() > xRaftNode.getState().getCurrentTerm()) {
						log.warn(
								"AppendEntriesResponseCallBack: received higher term:{} from peer:{}, current term:{}, change to follower, sequence:{}",
								response.getTerm(), raftPeer.getRaftPeerId(), xRaftNode.getState().getCurrentTerm(),
								inflightRequest.sequence);
						xRaftNode.getState().changeToFollower(response.getTerm());
						// 从流水线队列中移除该请求
						inflightRequests.remove(inflightRequest);
						// 清空流水线队列和待处理响应
						clearInflightRequests();
						pendingResponses.clear();
						return;
					}

					// 检查响应顺序性
					if (inflightRequest.sequence != nextResponseSequence.get()) {
						// 乱序响应：缓存起来，等待按序处理
						log.debug(
								"AppendEntriesResponseCallBack: out of order response, expected sequence:{}, got sequence:{}, caching for later, peer:{}",
								nextResponseSequence.get(), inflightRequest.sequence, raftPeer.getRaftPeerId());
						pendingResponses.put(inflightRequest.sequence, response);
						return;
					}

					// 按序处理当前响应
					processResponse(response, inflightRequest);

					// 尝试处理缓存的后续响应
					processBufferedResponses();
				}
				catch (Exception e) {
					log.error("AppendEntriesResponseCallBack error, sequence:{}", inflightRequest.sequence, e);
				}
			}

			/**
			 * 处理响应（成功或失败）
			 */
			private void processResponse(AppendEntriesResponse response, InflightRequest request) {
				// 从流水线队列中移除该请求
				inflightRequests.remove(request);

				// 处理成功响应
				if (response.isSuccess()) {
					// 按照 Raft 标准更新 matchIndex 和 nextIndex
					// matchIndex: 已知的已复制到该 follower 的最高日志索引
					matchIndex = request.getExpectMatchIndex();
					// nextIndex: 下一个要发送给该 follower 的日志索引
					// 保持不变式: nextIndex = matchIndex + 1
					nextIndex = request.getExpectNextIndex();

					// 更新下一个期望的响应序列号
					nextResponseSequence.incrementAndGet();
					// 退出探测模式
					probing = false;

					log.debug(
							"AppendEntries success to peer:{}, sequence:{}, matchIndex:{}, nextIndex:{}, startIndex:{}, endIndex:{}",
							raftPeer.getRaftPeerId(), request.sequence, matchIndex, nextIndex, request.startIndex,
							request.endIndex);

					// 尝试推进 commitIndex
					tryAdvanceCommitIndex();
				}
				else {
					// 处理失败响应：进入探测模式，从失败点位重新复制
					handleAppendFailure(response, request);
				}
			}

			/**
			 * 处理缓存的后续响应 按序处理所有已到达的连续响应
			 */
			private void processBufferedResponses() {
				while (true) {
					long expectedSeq = nextResponseSequence.get();
					AppendEntriesResponse bufferedResponse = pendingResponses.remove(expectedSeq);

					if (bufferedResponse == null) {
						// 没有更多连续的响应了
						break;
					}

					// 找到对应的请求
					InflightRequest bufferedRequest = null;
					for (InflightRequest req : inflightRequests) {
						if (req.sequence == expectedSeq) {
							bufferedRequest = req;
							break;
						}
					}

					if (bufferedRequest == null) {
						log.warn("Cannot find inflight request for buffered response, sequence:{}, peer:{}",
								expectedSeq, raftPeer.getRaftPeerId());
						// 跳过这个响应，继续处理下一个
						nextResponseSequence.incrementAndGet();
						continue;
					}

					log.debug("Processing buffered response, sequence:{}, peer:{}", expectedSeq,
							raftPeer.getRaftPeerId());
					processResponse(bufferedResponse, bufferedRequest);
				}
			}

			/**
			 * 处理日志复制失败 1. 清空流水线队列中的所有请求 2. 重置 nextIndex 和 nextSendIndex 到失败的位置 3.
			 * 进入探测模式，逐步回退找到匹配点
			 */
			private void handleAppendFailure(AppendEntriesResponse response, InflightRequest failedRequest) {
				// 清空流水线队列中的所有后续请求
				clearInflightRequests();

				// 进入探测模式
				probing = true;

				Long oldNextIndex = nextIndex;

				// 使用 conflictIndex 和 conflictTerm 优化日志回退
				if (response.getConflictIndex() != null && response.getConflictTerm() != null) {
					long conflictIndex = response.getConflictIndex();
					long conflictTerm = response.getConflictTerm();

					log.debug(
							"AppendEntries failed to peer:{}, sequence:{}, conflictIndex:{}, conflictTerm:{}, entering probing mode",
							raftPeer.getRaftPeerId(), failedRequest.sequence, conflictIndex, conflictTerm);

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

					log.info(
							"AppendEntries failed to peer:{}, sequence:{}, adjusted nextIndex from {} to {}, probing mode enabled",
							raftPeer.getRaftPeerId(), failedRequest.sequence, oldNextIndex, nextIndex);
				}
				else {
					// 没有冲突信息，保守回退到失败请求起始位置的前一个位置
					nextIndex = Math.max(0, failedRequest.startIndex - 1);
					log.info(
							"AppendEntries failed to peer:{}, sequence:{}, no conflict info, reset nextIndex from {} to {}, probing mode enabled",
							raftPeer.getRaftPeerId(), failedRequest.sequence, oldNextIndex, nextIndex);
				}

				// 确保 matchIndex 不超过 nextIndex - 1，维护 Raft 不变式
				if (matchIndex >= nextIndex) {
					matchIndex = nextIndex - 1;
					// should never happen
					log.error("Adjusted matchIndex to {} to maintain invariant (matchIndex < nextIndex) for peer:{}",
							matchIndex, raftPeer.getRaftPeerId());
				}

				// 同步 nextSendIndex 到 nextIndex
				nextSendIndex = nextIndex;

				// 重置序列号，从失败点位重新开始
				long currentSequence = nextSequence.get();
				nextSequence.set(failedRequest.sequence);
				nextResponseSequence.set(failedRequest.sequence);

				log.info("Reset sequence from {} to {} and nextSendIndex to {} for peer:{}", currentSequence,
						failedRequest.sequence, nextSendIndex, raftPeer.getRaftPeerId());
			}

			/**
			 * 清空流水线队列
			 */
			private void clearInflightRequests() {
				int cleared = inflightRequests.size();
				inflightRequests.clear();
				if (cleared > 0) {
					log.info("Cleared {} inflight requests for peer:{}", cleared, raftPeer.getRaftPeerId());
				}
				// 同时清空待处理的响应
				int clearedResponses = pendingResponses.size();
				pendingResponses.clear();
				if (clearedResponses > 0) {
					log.info("Cleared {} pending responses for peer:{}", clearedResponses, raftPeer.getRaftPeerId());
				}
			}

			@Override
			public void onException(Throwable throwable) {
				// 从流水线队列中移除该请求
				inflightRequests.remove(inflightRequest);

				log.error("AppendEntries to peer:{} exception, sequence:{}, startIndex:{}, endIndex:{}",
						raftPeer.getRaftPeerId(), inflightRequest.sequence, inflightRequest.startIndex,
						inflightRequest.endIndex, throwable);

				// 发生异常，重置 nextIndex 到失败的起始位置，准备重试
				nextIndex = inflightRequest.startIndex;
				// 同步 nextSendIndex 到 nextIndex
				nextSendIndex = nextIndex;

				// 清空流水线队列和待处理响应
				inflightRequests.clear();
				pendingResponses.clear();

				// 进入探测模式
				probing = true;

				// 重置序列号
				nextSequence.set(inflightRequest.sequence);
				nextResponseSequence.set(inflightRequest.sequence);

				log.info(
						"Reset nextIndex to {} and nextSendIndex to {} and sequence to {} due to exception for peer:{}",
						nextIndex, nextSendIndex, inflightRequest.sequence, raftPeer.getRaftPeerId());
			}

		}

	}

	/**
	 * // todo： 优化 尝试推进 commitIndex 根据 Raft 论文 §5.3 和 §5.4： If there exists an N such that
	 * N > commitIndex, a majority of matchIndex[i] ≥ N, and log[N].term == currentTerm:
	 * set commitIndex = N
	 */
	private void tryAdvanceCommitIndex() {
		try {
			xRaftNode.getState().getWriteLock().lock();

			// 只有 Leader 才能推进 commitIndex
			if (xRaftNode.getState().getRole() != RaftRole.LEADER) {
				return;
			}

			RaftLog raftLog = xRaftNode.getState().getRaftLog();
			Long currentCommitIndex = xRaftNode.getState().getCommitIndex();
			Long lastLogIndex = raftLog.getLastEntryTermIndex().getIndex();

			// 从 commitIndex + 1 开始，尝试找到可以提交的最大索引
			for (long N = lastLogIndex; N > currentCommitIndex; N--) {
				LogEntry logEntry = raftLog.getEntry(N);
				if (logEntry == null) {
					continue;
				}

				// 只能提交当前 term 的日志（Raft 论文 §5.4.2）
				// 这是为了避免提交之前 term 的日志导致的安全性问题
				if (logEntry.term() != xRaftNode.getState().getCurrentTerm()) {
					continue;
				}

				// 统计有多少个节点已经复制了索引 N 的日志
				int replicatedCount = 1; // Leader 自己算一个

				for (LogReplicator replicator : logReplicators.values()) {
					if (replicator.getMatchIndex() >= N) {
						replicatedCount++;
					}
				}

				// 获取集群配置
				int clusterSize = xRaftNode.getState().getConfigState().getCurrentConfig().getConf().getPeers().size();

				// 检查是否达到多数派
				int majority = clusterSize / 2 + 1;
				if (replicatedCount >= majority) {
					// 找到了可以提交的索引 N
					xRaftNode.getState().setCommitIndex(N);
					log.info("Advanced commitIndex to {} (replicatedCount={}, majority={}, clusterSize={})", N,
							replicatedCount, majority, clusterSize);
					return;
				}
			}
		}
		finally {
			xRaftNode.getState().getWriteLock().unlock();
		}
	}

}
