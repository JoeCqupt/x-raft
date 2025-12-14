package io.github.xinfra.lab.raft;

import lombok.Data;

@Data
public class RaftGroupOptions {

	String raftGroupId;

	private RaftNodeOptions raftNodeOptions;

}
