package io.github.xinfra.lab.raft.core;

import io.github.xinfra.lab.raft.AbstractLifeCycle;
import io.github.xinfra.lab.raft.core.transport.RaftApi;
import io.github.xinfra.lab.raft.transport.TransportServer;
import io.github.xinfra.lab.raft.transport.TransportType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class XRaftServer extends AbstractLifeCycle {

	private RaftServerOptions raftServerOptions;

	private TransportServer transportServer;

	private Map<String, XRaftGroup> raftGroupMap = new HashMap<>();

	public XRaftServer(RaftServerOptions raftServerOptions) {
		this.raftServerOptions = raftServerOptions;
	}

	@Override
	public void startup() {
		super.startup();
		TransportType transportType = raftServerOptions.getTransportType();
		transportServer = transportType.newServer(raftServerOptions.getTransportServerOptions());
		// todo @joecqupt
		transportServer.registerRequestHandler(RaftApi.requestVote, null);
		// todo @joecqupt
		transportServer.registerRequestHandler(RaftApi.appendEntries, null);
		transportServer.startup();
	}

	@Override
	public void shutdown() {
		super.shutdown();
		raftGroupMap.values().forEach(XRaftGroup::shutdown);
		transportServer.shutdown();
	}

	public XRaftGroup startRaftGroup(RaftGroupOptions raftGroupOptions) {
		if (raftGroupMap.containsKey(raftGroupOptions.getRaftGroupId())) {
			throw new IllegalArgumentException("raft group already exists");
		}
		XRaftGroup raftGroup = new XRaftGroup(raftGroupOptions);
		raftGroup.startup();
		raftGroupMap.put(raftGroupOptions.getRaftGroupId(), raftGroup);
		return raftGroup;
	}

	public List<XRaftGroup> getRaftGroups() {
		return raftGroupMap.values().stream().collect(Collectors.toList());
	}

}
