package io.github.xinfra.lab.raft;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class RaftGroup {

	private String raftGroupId;

	private List<RaftPeer> raftPeers;

	private List<RaftPeer> learners;

	public RaftGroup(String raftGroupId, List<RaftPeer> raftPeers) {
		this.raftGroupId = raftGroupId;
		this.raftPeers = raftPeers;
	}

}
