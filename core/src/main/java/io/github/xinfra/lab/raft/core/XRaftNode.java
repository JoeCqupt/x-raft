package io.github.xinfra.lab.raft.core;

import com.google.common.base.Verify;
import io.github.xinfra.lab.raft.AbstractLifeCycle;
import io.github.xinfra.lab.raft.RaftNode;
import io.github.xinfra.lab.raft.RaftNodeOptions;
import io.github.xinfra.lab.raft.RaftPeer;
import io.github.xinfra.lab.raft.RaftRole;
import io.github.xinfra.lab.raft.core.common.Responses;
import io.github.xinfra.lab.raft.conf.Configuration;
import io.github.xinfra.lab.raft.conf.ConfigurationEntry;
import io.github.xinfra.lab.raft.core.conf.RaftConfigurationState;
import io.github.xinfra.lab.raft.core.state.RaftNodeState;
import io.github.xinfra.lab.raft.log.RaftLog;
import io.github.xinfra.lab.raft.protocol.AppendEntriesRequest;
import io.github.xinfra.lab.raft.protocol.AppendEntriesResponse;
import io.github.xinfra.lab.raft.protocol.SetConfigurationRequest;
import io.github.xinfra.lab.raft.protocol.SetConfigurationResponse;
import io.github.xinfra.lab.raft.protocol.VoteRequest;
import io.github.xinfra.lab.raft.protocol.VoteResponse;
import io.github.xinfra.lab.raft.transport.TransportClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

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
			state.changeToFollower();
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
		// todo
		return null;
	}

	@Override
	public VoteResponse handleVoteRequest(VoteRequest voteRequest) {
		Verify.verify(isStarted(), "RaftNode is not started yet.");
		// todo verify raft group

		VoteContext voteContext = new VoteContext(this, voteRequest);
		boolean voteGranted = voteContext.decideVote();
		// todo
		boolean shouldShutdown = false;

		return Responses.voteResponse(voteRequest.getPeerId(), voteRequest.getReplyPeerId(),
				state.getCurrentTerm().get(), voteGranted, shouldShutdown);
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

}
