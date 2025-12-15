package io.github.xinfra.lab.raft.core.transport;

import io.github.xinfra.lab.raft.RaftGroup;
import io.github.xinfra.lab.raft.protocol.VoteRequest;
import io.github.xinfra.lab.raft.protocol.VoteResponse;
import io.github.xinfra.lab.raft.transport.RequestHandler;

import java.util.Map;

public class RaftRequestHandler implements RequestHandler<VoteRequest, VoteResponse> {

	Map<String, RaftGroup> raftGroupMap;

	@Override
	public VoteResponse handle(VoteRequest request) {
		String raftGroupId = request.getRaftGroupId();
		RaftGroup raftGroup = raftGroupMap.get(raftGroupId);
		if (raftGroup == null) {
			// todo: handle error
			return null;
		}
		return null;
	}

}
