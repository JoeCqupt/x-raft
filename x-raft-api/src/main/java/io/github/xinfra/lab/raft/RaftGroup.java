package io.github.xinfra.lab.raft;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class RaftGroup {

	private String raftGroupId;

	private Set<RaftPeer> peers;

	public RaftGroup(String raftGroupId, Set<RaftPeer> peers) {
		this.raftGroupId = raftGroupId;
		this.peers = peers;
	}

}
