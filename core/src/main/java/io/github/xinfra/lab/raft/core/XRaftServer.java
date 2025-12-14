package io.github.xinfra.lab.raft.core;

import io.github.xinfra.lab.raft.AbstractLifeCycle;
import io.github.xinfra.lab.raft.RaftGroup;
import io.github.xinfra.lab.raft.RaftGroupOptions;
import io.github.xinfra.lab.raft.RaftServer;
import io.github.xinfra.lab.raft.RaftServerOptions;
import io.github.xinfra.lab.raft.core.transport.RaftApi;
import io.github.xinfra.lab.raft.transport.TransportServer;
import io.github.xinfra.lab.raft.transport.TransportType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class XRaftServer extends AbstractLifeCycle implements RaftServer {

	private RaftServerOptions raftServerOptions;

	private TransportServer transportServer;

	private Map<String, RaftGroup> raftGroupMap = new HashMap<>();

	public XRaftServer(RaftServerOptions raftServerOptions) {
		this.raftServerOptions = raftServerOptions;
	}

	@Override
	public void startup() {
		super.startup();
		TransportType transportType = raftServerOptions.getTransportType();
		TransportServer transportServer = transportType.newServer(raftServerOptions.getTransportServerOptions());
		// todo @joecqupt
		transportServer.registerRequestHandler(RaftApi.requestVote, null);
		// todo @joecqupt
		transportServer.registerRequestHandler(RaftApi.appendEntries, null);
		transportServer.startup();
	}

	@Override
	public void shutdown() {
		super.shutdown();
		transportServer.shutdown();
		raftGroupMap.values().forEach(RaftGroup::shutdown);
	}

	@Override
	public void startRaftGroup(RaftGroupOptions raftGroupOptions) {
		ensureStarted();
		if (raftGroupMap.containsKey(raftGroupOptions.getRaftGroupId())) {
			throw new IllegalArgumentException("raft group already exists");
		}
		RaftGroup raftGroup = new XRaftGroup(raftGroupOptions);
		raftGroup.startup();
		raftGroupMap.put(raftGroupOptions.getRaftGroupId(), raftGroup);
	}

	@Override
	public List<RaftGroup> getRaftGroups() {
		return raftGroupMap.values().stream().collect(Collectors.toList());
	}

}
