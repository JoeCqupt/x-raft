package io.github.xinfra.lab.raft.protocol;

import lombok.Data;

import java.io.Serializable;

@Data
public abstract class Message implements Serializable {

	String raftGroupId;

	// todo:
	String requestPeerId;

	// todo
	String replyPeerId;

	boolean success;

	/**
	 * only used when success is false
	 */
	ErrorInfo errorInfo;

}
