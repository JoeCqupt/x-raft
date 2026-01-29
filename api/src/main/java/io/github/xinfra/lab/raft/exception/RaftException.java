package io.github.xinfra.lab.raft.exception;

public class RaftException extends RuntimeException {

	private int errorCode;

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
