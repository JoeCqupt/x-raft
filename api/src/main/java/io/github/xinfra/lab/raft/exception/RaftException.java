package io.github.xinfra.lab.raft.exception;

import io.github.xinfra.lab.raft.common.RaftErrorCode;

public class RaftException extends RuntimeException {

	private int errorCode;

	public RaftException(RaftErrorCode raftErrorCode) {
		this(raftErrorCode.getCode(), raftErrorCode.getMsg());
	}

	public RaftException(int errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public RaftException(int errorCode, String message, Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode;
	}

	public RaftException(int errorCode, Throwable cause) {
		super(cause);
		this.errorCode = errorCode;
	}

}
