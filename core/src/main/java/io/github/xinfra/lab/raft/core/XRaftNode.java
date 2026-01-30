package io.github.xinfra.lab.raft.core;

import io.github.xinfra.lab.raft.AbstractLifeCycle;
import io.github.xinfra.lab.raft.RaftNode;
import io.github.xinfra.lab.raft.RaftNodeOptions;
import io.github.xinfra.lab.raft.RaftPeer;
import io.github.xinfra.lab.raft.RaftRole;
import io.github.xinfra.lab.raft.conf.Configuration;
import io.github.xinfra.lab.raft.conf.ConfigurationEntry;
import io.github.xinfra.lab.raft.core.common.Responses;
import io.github.xinfra.lab.raft.core.conf.RaftConfigurationState;
import io.github.xinfra.lab.raft.core.state.RaftNodeState;
import io.github.xinfra.lab.raft.log.LogEntry;
import io.github.xinfra.lab.raft.log.RaftLog;

import java.util.List;
import io.github.xinfra.lab.raft.log.TermIndex;
import io.github.xinfra.lab.raft.protocol.AppendEntriesRequest;
import io.github.xinfra.lab.raft.protocol.AppendEntriesResponse;
import io.github.xinfra.lab.raft.protocol.SetConfigurationRequest;
import io.github.xinfra.lab.raft.protocol.SetConfigurationResponse;
import io.github.xinfra.lab.raft.protocol.VoteRequest;
import io.github.xinfra.lab.raft.protocol.VoteResponse;
import io.github.xinfra.lab.raft.transport.TransportClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class XRaftNode extends AbstractLifeCycle implements RaftNode {

	private String raftGroupId;

	private RaftPeer raftPeer;

	@Getter
	private RaftNodeOptions raftNodeOptions;

	@Getter
	private RaftNodeState state;

	@Getter
	private TransportClient transportClient;

	public XRaftNode(String raftGroupId, RaftNodeOptions raftNodeOptions) {
		this.raftGroupId = raftGroupId;
		this.raftPeer = raftNodeOptions.getRaftPeer();
		this.raftNodeOptions = raftNodeOptions;
		if (raftNodeOptions.isShareTransportClientFlag()) {
			this.transportClient = raftNodeOptions.getShareTransportClient();
		}
		else {
			this.transportClient = raftNodeOptions.getTransportType()
				.newClient(raftNodeOptions.getTransportClientOptions());
		}
		RaftLog raftLog = raftNodeOptions.getRaftLogType().newRaftLog(this);

		Configuration initialConf = raftNodeOptions.getInitialConf();
		ConfigurationEntry initialConfiguration = new ConfigurationEntry(null, initialConf);
		RaftConfigurationState configState = new RaftConfigurationState(initialConfiguration);
		this.state = new RaftNodeState(this, raftLog, configState);
	}

	@Override
	public AppendEntriesResponse handleAppendEntries(AppendEntriesRequest appendEntriesRequest) {
		try {
			state.getWriteLock().lock();

			AppendEntriesResponse response = new AppendEntriesResponse();
			response.setTerm(state.getCurrentTerm());
			response.setSuccess(false);

			// 1. Reply false if term < currentTerm (§5.1)
			if (appendEntriesRequest.getTerm() < state.getCurrentTerm()) {
				log.warn("reject appendEntries from {} because request term:{} < currentTerm:{}",
						appendEntriesRequest.getLeaderId(), appendEntriesRequest.getTerm(), state.getCurrentTerm());
				return response;
			}

			// 2. If RPC request or response contains term T > currentTerm: set
			// currentTerm = T, convert to follower (§5.1)
			if (appendEntriesRequest.getTerm() > state.getCurrentTerm()) {
				log.info("receive appendEntries from {} with higher term:{}, currentTerm:{}, convert to follower",
						appendEntriesRequest.getLeaderId(), appendEntriesRequest.getTerm(), state.getCurrentTerm());
				state.changeToFollower(appendEntriesRequest.getTerm());
				state.setLeaderId(appendEntriesRequest.getLeaderId());
				response.setTerm(state.getCurrentTerm());
			}

			// 3. Validate leader peer
			RaftPeer leaderPeer = state.getConfigState()
				.getCurrentConfig()
				.getRaftPeer(appendEntriesRequest.getLeaderId());
			if (leaderPeer == null) {
				log.warn("reject appendEntries from {} because leader not in configuration",
						appendEntriesRequest.getLeaderId());
				return response;
			}

			// 4. Update last leader RPC time (reset election timer)
			state.setLastLeaderRpcTimeMills(System.currentTimeMillis());

			// 5. If we're a candidate, convert to follower
			if (state.getRole() == RaftRole.CANDIDATE) {
				log.info("candidate receive appendEntries from leader {}, convert to follower",
						appendEntriesRequest.getLeaderId());
				state.changeToFollower(appendEntriesRequest.getTerm());
				state.setLeaderId(appendEntriesRequest.getLeaderId());
				response.setTerm(state.getCurrentTerm());
			}

			RaftLog raftLog = state.getRaftLog();

			// 6. Reply false if log doesn't contain an entry at prevLogIndex whose term
			// matches prevLogTerm (§5.3)
			long prevLogIndex = appendEntriesRequest.getPrevLogIndex();
			long prevLogTerm = appendEntriesRequest.getPrevLogTerm();

			if (prevLogIndex >= 0) {
				LogEntry prevLogEntry = raftLog.getEntry(prevLogIndex);
				if (prevLogEntry == null) {
					// 优化：日志不存在，返回当前日志的最后索引
					TermIndex lastTermIndex = raftLog.getLastEntryTermIndex();
					response.setConflictIndex(lastTermIndex.getIndex() + 1);
					response.setConflictTerm(0L);
					log.warn(
							"reject appendEntries from {} because prevLogEntry not found at index:{}, conflictIndex:{}",
							appendEntriesRequest.getLeaderId(), prevLogIndex, response.getConflictIndex());
					return response;
				}
				if (prevLogEntry.term() != prevLogTerm) {
					// 优化：日志 term 不匹配，返回冲突 term 的第一个索引
					long conflictTerm = prevLogEntry.term();
					long conflictIndex = prevLogIndex;

					// 向前查找该 term 的第一个索引
					for (long i = prevLogIndex - 1; i >= 0; i--) {
						LogEntry entry = raftLog.getEntry(i);
						if (entry == null || entry.term() != conflictTerm) {
							conflictIndex = i + 1;
							break;
						}
						if (i == 0) {
							conflictIndex = 0;
						}
					}

					response.setConflictIndex(conflictIndex);
					response.setConflictTerm(conflictTerm);
					log.warn(
							"reject appendEntries from {} because prevLogEntry term mismatch, expected:{}, actual:{}, conflictIndex:{}, conflictTerm:{}",
							appendEntriesRequest.getLeaderId(), prevLogTerm, prevLogEntry.term(), conflictIndex,
							conflictTerm);
					return response;
				}
			}

			// 7. Append new entries with conflict resolution
			// If an existing entry conflicts with a new one (same index but different
			// terms),
			// delete the existing entry and all that follow it (§5.3)
			List<LogEntry> entries = appendEntriesRequest.getEntries();
			if (entries != null && !entries.isEmpty()) {
				// 验证日志条目的连续性
				long expectedIndex = prevLogIndex + 1;
				for (int i = 0; i < entries.size(); i++) {
					LogEntry entry = entries.get(i);
					if (entry.index() != expectedIndex) {
						log.error(
								"Log entries are not continuous, expected index:{}, actual index:{}, rejecting appendEntries from {}",
								expectedIndex, entry.index(), appendEntriesRequest.getLeaderId());
						response.setSuccess(false);
						return response;
					}
					expectedIndex++;
				}

				// 处理日志冲突和追加
				boolean conflictFound = false;
				for (int i = 0; i < entries.size(); i++) {
					LogEntry entry = entries.get(i);
					long entryIndex = entry.index();

					if (conflictFound) {
						// 已经发现冲突并截断，直接追加后续所有日志
						raftLog.append(entry);
						log.debug("appended entry at index:{}, term:{} after conflict resolution", entry.index(),
								entry.term());
					}
					else {
						LogEntry existingEntry = raftLog.getEntry(entryIndex);

						if (existingEntry != null) {
							// 检查是否存在冲突
							if (existingEntry.term() != entry.term()) {
								// 发现冲突：删除该位置及之后的所有日志
								log.warn(
										"Log conflict detected at index:{}, existing term:{}, new term:{}, truncating logs after index:{}",
										entryIndex, existingEntry.term(), entry.term(), entryIndex - 1);
								raftLog.truncateAfter(entryIndex - 1);
								conflictFound = true;

								// 追加新日志
								raftLog.append(entry);
								log.debug("appended entry at index:{}, term:{} after truncation", entry.index(),
										entry.term());
							}
							else {
								// 相同的日志条目，跳过（幂等性）
								log.debug("skipped duplicate entry at index:{}, term:{}", entry.index(), entry.term());
							}
						}
						else {
							// 该位置没有日志，验证是否连续
							long lastLogIndex = raftLog.getLastEntryTermIndex().getIndex();
							if (entryIndex != lastLogIndex + 1) {
								log.error(
										"Gap detected in log, last log index:{}, new entry index:{}, rejecting appendEntries from {}",
										lastLogIndex, entryIndex, appendEntriesRequest.getLeaderId());
								response.setSuccess(false);
								return response;
							}

							// 直接追加
							raftLog.append(entry);
							log.debug("appended entry at index:{}, term:{}", entry.index(), entry.term());
						}
					}
				}
			}

			// 8. Update commit index if leader's commit index is higher
			if (appendEntriesRequest.getLeaderCommit() > state.getCommitIndex()) {
				long newCommitIndex = Math.min(appendEntriesRequest.getLeaderCommit(),
						raftLog.getLastEntryTermIndex().getIndex());
				state.setCommitIndex(newCommitIndex);
				log.debug("updated commitIndex to {}", newCommitIndex);
			}

			// 9. Success
			response.setSuccess(true);
			return response;

		}
		finally {
			state.getWriteLock().unlock();
		}
	}

	@Override
	public String getRaftGroupId() {
		return raftGroupId;
	}

	@Override
	public String getRaftPeerId() {
		return raftPeer.getRaftPeerId();
	}

	@Override
	public String getRaftGroupPeerId() {
		return String.format("[%s]-[%s]", raftGroupId, getRaftPeerId());
	}

	public RaftPeer getRaftPeer() {
		return raftPeer;
	}

	@Override
	public RaftRole getRaftRole() {
		return state.getRole();
	}

    @Override
    public void notifyLogAppended() {
        state.notifyLogAppended();
    }


    @Override
	public void startup() {
		super.startup();
		// todo: init raft storage - check lock
		// todo: init raft log - check lock
		// todo: init state machine - check lock
		state.updateConfiguration();
		if (!raftNodeOptions.isShareTransportClientFlag()) {
			transportClient.startup();
		}
		// todo: connect to peers
		try {
			state.getWriteLock().lock();
			RaftPeer raftPeer = state.getConfigState().getCurrentConfig().getRaftPeer(getRaftPeerId());
			if (raftPeer != null) {
				state.changeToFollower();
			}
			else {
				// not in config : start up as learner
				state.changeToLearner();
			}
		}
		finally {
			state.getWriteLock().unlock();
		}
	}

	@Override
	public void shutdown() {
		super.shutdown();
		if (!raftNodeOptions.isShareTransportClientFlag()) {
			transportClient.shutdown();
		}
		// todo: check
	}

	@Override
	public VoteResponse handlePreVoteRequest(VoteRequest voteRequest) {
		try {
			state.getWriteLock().lock();
			boolean granted = false;
			Long term = state.getCurrentTerm();
			if (state.getRole() == RaftRole.LEARNER) {
				log.info("handleVoteRequest reject: because the role is learner");
				return Responses.voteResponse(granted, term);
			}
			String candidatePeerId = voteRequest.getCandidateId();
			RaftPeer candidatePeer = state.getConfigState().getCurrentConfig().getRaftPeer(candidatePeerId);
			if (candidatePeer == null) {
				log.info("handlePreVoteRequest reject: candidateId:{} not found.", candidatePeerId);
				return Responses.voteResponse(granted, term);
			}
			if (isCurrentLeaderValid()) {
				log.info("handlePreVoteRequest reject: because the leader:{} still alive", state.getLeaderId());
				return Responses.voteResponse(granted, term);
			}
			if (voteRequest.getTerm() < term) {
				log.info("handlePreVoteRequest reject: because the term:{} is smaller than current term:{}",
						voteRequest.getTerm(), term);
				// todo check log appender
				return Responses.voteResponse(granted, term);
			}

			TermIndex lastEntryTermIndex = state.getRaftLog().getLastEntryTermIndex();
			if (voteRequest.getLastLogIndex() >= lastEntryTermIndex.getIndex()
					&& voteRequest.getLastLogTerm() >= lastEntryTermIndex.getTerm()) {
				granted = true;
			}
			return Responses.voteResponse(granted, term);
		}
		finally {
			state.getWriteLock().unlock();
		}
	}

	@Override
	public VoteResponse handleVoteRequest(VoteRequest voteRequest) {
		try {
			state.getWriteLock().lock();
			boolean granted = false;
			Long term = state.getCurrentTerm();
			if (state.getRole() == RaftRole.LEARNER) {
				log.info("handleVoteRequest reject: because the role is learner");
				return Responses.voteResponse(granted, term);
			}
			String candidatePeerId = voteRequest.getCandidateId();
			RaftPeer candidatePeer = state.getConfigState().getCurrentConfig().getRaftPeer(candidatePeerId);
			if (candidatePeer == null) {
				log.info("handleVoteRequest reject: candidateId:{} not found.", candidatePeerId);
				return Responses.voteResponse(granted, term);
			}
			if (voteRequest.getTerm() < term) {
				log.info("handleVoteRequest reject: because the term:{} is smaller than current term:{}",
						voteRequest.getTerm(), term);
				return Responses.voteResponse(granted, term);
			}
			if (voteRequest.getTerm() > term) {
				log.info("handleVoteRequest receive higher term:{} current term:{}  change to follower",
						voteRequest.getTerm(), term);
				state.changeToFollower(voteRequest.getTerm());
			}

			TermIndex lastEntryTermIndex = state.getRaftLog().getLastEntryTermIndex();
			if (voteRequest.getLastLogIndex() >= lastEntryTermIndex.getIndex()
					&& voteRequest.getLastLogTerm() >= lastEntryTermIndex.getTerm()) {
				if (StringUtils.isBlank(state.getVotedFor())) {
					state.setVotedFor(voteRequest.getCandidateId());
					state.persistMetadata();
					state.changeToFollower();
					granted = true;
				}
				else if (state.getVotedFor().equals(voteRequest.getCandidateId())) {
					granted = true;
				}
			}
			return Responses.voteResponse(granted, term);
		}
		finally {
			state.getWriteLock().unlock();
		}
	}

	@Override
	public SetConfigurationResponse setRaftConfiguration(SetConfigurationRequest request) {
		// todo
		return null;
	}

	private boolean isCurrentLeaderValid() {
		return StringUtils.isNoneBlank(state.getLeaderId()) && System.currentTimeMillis()
				- state.getLastLeaderRpcTimeMills() < raftNodeOptions.getElectionTimeoutMills();
	}

}
