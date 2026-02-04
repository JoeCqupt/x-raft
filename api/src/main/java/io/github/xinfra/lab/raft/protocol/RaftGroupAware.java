package io.github.xinfra.lab.raft.protocol;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@Data
@ToString
public class RaftGroupAware implements Serializable {

	/**
	 * request target raft group id
	 */
	String raftGroupId;

	/**
	 * request target raft peer id
	 */
	String raftPeerId;

}
