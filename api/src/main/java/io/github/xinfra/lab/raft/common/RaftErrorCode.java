package io.github.xinfra.lab.raft.common;

import lombok.Getter;

public enum RaftErrorCode {

	UNKNOWN_ERROR(0, "unknown error"), SUCCESS(1, "success"), NODE_NOT_FOUND(2, "node not found"),
	NODE_NOT_IN_CONF(3, "node not in conf");

	@Getter
	private final int code;

	@Getter
	public final String msg;

	RaftErrorCode(int code, String msg) {
		this.code = code;
		this.msg = msg;
	}

}
