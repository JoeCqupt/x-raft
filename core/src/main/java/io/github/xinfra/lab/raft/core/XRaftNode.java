package io.github.xinfra.lab.raft.core;

import com.google.common.base.Verify;
import io.github.xinfra.lab.raft.AbstractLifeCycle;
import io.github.xinfra.lab.raft.RaftNode;
import io.github.xinfra.lab.raft.RaftNodeOptions;
import io.github.xinfra.lab.raft.RaftPeerId;
import io.github.xinfra.lab.raft.RaftRole;
import io.github.xinfra.lab.raft.core.common.Responses;
import io.github.xinfra.lab.raft.core.conf.Configuration;
import io.github.xinfra.lab.raft.core.conf.ConfigurationEntry;
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

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class XRaftNode extends AbstractLifeCycle implements RaftNode {

	private String raftGroupId;

	private RaftPeerId raftPeerId;

	@Getter
	private RaftNodeOptions raftNodeOptions;

	private RaftLog raftLog;

	@Getter
	private RaftNodeState state;

	@Getter
	private RaftConfigurationState configState;

	@Getter
	private TransportClient transportClient;

	public XRaftNode(String raftGroupId, RaftNodeOptions raftNodeOptions) {
		this.raftGroupId = raftGroupId;
		this.raftPeerId = raftNodeOptions.getRaftPeerId();
		this.raftNodeOptions = raftNodeOptions;
		if (raftNodeOptions.isShareTransportClientFlag()) {
			this.transportClient = raftNodeOptions.getShareTransportClient();
		}
		else {
			this.transportClient = raftNodeOptions.getTransportType()
				.newClient(raftNodeOptions.getTransportClientOptions());
		}
		this.raftLog = raftNodeOptions.getRaftLogType().newRaftLog(this);
		this.state = new RaftNodeState(this);

		// init raft configuration
		Configuration conf = new Configuration(raftNodeOptions.getPeers(), raftNodeOptions.getLearners());
		ConfigurationEntry initialConfiguration = new ConfigurationEntry(conf, null);
		this.configState = new RaftConfigurationState(initialConfiguration);
	}

	@Override
	public String raftGroupId() {
		return raftGroupId;
	}

	@Override
	public RaftPeerId raftPeerId() {
		return raftPeerId;
	}

	@Override
	public RaftLog raftLog() {
		return raftLog;
	}

	@Override
	public RaftRole raftRole() {
		return state.getRole();
	}

	@Override
	public synchronized void startup() {
		super.startup();
		// todo: init raft storage
		// todo: init raft log
		// todo: init state machine
		configState.checkAndSetCurrentConfiguration();
		if (!raftNodeOptions.isShareTransportClientFlag()) {
			transportClient.startup();
		}
		// todo: connect to peers
		state.changeToFollower();
	}

	@Override
	public synchronized void shutdown() {
		super.shutdown();
		if (!raftNodeOptions.isShareTransportClientFlag()) {
			transportClient.shutdown();
		}
		// todo: check
	}

	@Override
	public VoteResponse requestVote(VoteRequest voteRequest) {
		Verify.verify(isStarted(), "RaftNode is not started yet.");
		// todo verify raft group

		VoteContext voteContext = new VoteContext(this, voteRequest);
		boolean voteGranted = voteContext.decideVote();
		// todo
		boolean shouldShutdown = false;

		return Responses.voteResponse(voteRequest.getRequestPeerId(), voteRequest.getReplyPeerId(),
				state.getCurrentTerm().get(), voteGranted, shouldShutdown);
	}

	@Override
	public AppendEntriesResponse appendEntries(AppendEntriesRequest appendEntriesRequest) {

		// todo
		return null;
	}

	@Override
	public SetConfigurationResponse setRaftConfiguration(SetConfigurationRequest request) {
		// todo
		return null;
	}

	public Long getRandomElectionTimeoutMills() {
		Long timeoutMills = raftNodeOptions.getElectionTimeoutMills();
		Long delayMills = raftNodeOptions.getElectionTimeoutDelayMills();
		return ThreadLocalRandom.current().nextLong(timeoutMills, timeoutMills + delayMills);
	}

}
