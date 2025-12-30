package io.github.xinfra.lab.raft.core;

import io.github.xinfra.lab.raft.AbstractLifeCycle;
import io.github.xinfra.lab.raft.RaftNode;
import io.github.xinfra.lab.raft.RaftNodeOptions;
import io.github.xinfra.lab.raft.RaftPeer;
import io.github.xinfra.lab.raft.RaftRole;
import io.github.xinfra.lab.raft.common.RaftError;
import io.github.xinfra.lab.raft.core.common.Responses;
import io.github.xinfra.lab.raft.conf.Configuration;
import io.github.xinfra.lab.raft.conf.ConfigurationEntry;
import io.github.xinfra.lab.raft.core.common.VoteResponses;
import io.github.xinfra.lab.raft.core.conf.RaftConfigurationState;
import io.github.xinfra.lab.raft.core.state.RaftNodeState;
import io.github.xinfra.lab.raft.log.RaftLog;
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
	public String getRaftGroupId() {
		return raftGroupId;
	}

	@Override
	public String getRaftPeerId() {
		return raftPeer.getRaftPeerId();
	}

	@Override
	public String getRaftGroupPeerId() {
		return  String.format("[%s]-[%s]", raftGroupId, getRaftPeerId());
	}

	public RaftPeer getRaftPeer() {
		return raftPeer;
	}

	@Override
	public RaftRole getRaftRole() {
		return state.getRole();
	}

	@Override
	public void startup() {
		super.startup();
		// todo: init raft storage
		// todo: init raft log
		// todo: init state machine
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
            } else {
                // not in config
                state.changeToLearner();
            }
		}finally {
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
			if (state.getRole() == RaftRole.LEARNER){
				log.info("handleVoteRequest reject: because the role is learner");
				return VoteResponses.voteResponse(granted, state.getCurrentTerm());
			}
            String candidatePeerId = voteRequest.getCandidateId();
            RaftPeer candidatePeer = state.getConfigState().getCurrentConfig().getRaftPeer(candidatePeerId);
            if (candidatePeer == null) {
                log.info("handlePreVoteRequest reject: candidateId:{} not found.", candidatePeerId);
                return VoteResponses.voteResponse(granted, state.getCurrentTerm());
            }
            if(isCurrentLeaderValid()){
                log.info("handlePreVoteRequest reject: because the leader:{} still alive", state.getLeaderId());
                return VoteResponses.voteResponse(granted, state.getCurrentTerm());
            }
            if (voteRequest.getTerm() < state.getCurrentTerm()){
                log.info("handlePreVoteRequest reject: because the term:{} is smaller than current term:{}", voteRequest.getTerm(), state.getCurrentTerm());
                // todo check log appender
                return VoteResponses.voteResponse(granted, state.getCurrentTerm());
            }
            TermIndex lastEntryTermIndex = state.getRaftLog().getLastEntryTermIndex();
            if (voteRequest.getLastLogIndex()>= lastEntryTermIndex.getIndex() && voteRequest.getLastLogTerm()>=lastEntryTermIndex.getTerm()) {
                granted = true;
            }
            return VoteResponses.voteResponse(granted, state.getCurrentTerm());
        } finally {
            state.getWriteLock().unlock();
        }
	}

    @Override
	public VoteResponse handleVoteRequest(VoteRequest voteRequest) {
        try {
            state.getWriteLock().lock();
            boolean granted = false;
			if (state.getRole() == RaftRole.LEARNER){
				log.info("handleVoteRequest reject: because the role is learner");
				return VoteResponses.voteResponse(granted, state.getCurrentTerm());
			}
			String candidatePeerId = voteRequest.getCandidateId();
			RaftPeer candidatePeer = state.getConfigState().getCurrentConfig().getRaftPeer(candidatePeerId);
			if (candidatePeer == null) {
				log.info("handleVoteRequest reject: candidateId:{} not found.", candidatePeerId);
				return VoteResponses.voteResponse(granted, state.getCurrentTerm());
			}
            if (voteRequest.getTerm() < state.getCurrentTerm()) {
                log.info("handleVoteRequest reject: because the term:{} is smaller than current term:{}", voteRequest.getTerm(), state.getCurrentTerm());
                return VoteResponses.voteResponse(granted, state.getCurrentTerm());
            }
            if (voteRequest.getTerm() > state.getCurrentTerm()){
                log.info("handleVoteRequest receive higher term:{} current term:{}  change to follower", voteRequest.getTerm(), state.getCurrentTerm());
                state.changeToFollower(voteRequest.getTerm());
            }

            TermIndex lastEntryTermIndex = state.getRaftLog().getLastEntryTermIndex();

            if (voteRequest.getLastLogIndex() >= lastEntryTermIndex.getIndex() && voteRequest.getLastLogTerm() >= lastEntryTermIndex.getTerm()) {
                if (StringUtils.isBlank(state.getVotedFor())){
                    state.setVotedFor(voteRequest.getCandidateId());
                    state.persistMetadata();
                    state.changeToFollower();
                    granted = true;
                } else if (state.getVotedFor().equals(voteRequest.getCandidateId())){
                    granted = true;
                }
            }
            return VoteResponses.voteResponse(granted, state.getCurrentTerm());
        } finally {
            state.getWriteLock().unlock();
        }
	}

	@Override
	public AppendEntriesResponse handleAppendEntries(AppendEntriesRequest appendEntriesRequest) {

		// todo
		return null;
	}

	@Override
	public SetConfigurationResponse setRaftConfiguration(SetConfigurationRequest request) {
		// todo
		return null;
	}

    private boolean isCurrentLeaderValid() {
        return StringUtils.isNoneBlank(state.getLeaderId()) &&
                System.currentTimeMillis() - state.getLastLeaderRpcTimeMills() < raftNodeOptions.getElectionTimeoutMills();
    }
}
