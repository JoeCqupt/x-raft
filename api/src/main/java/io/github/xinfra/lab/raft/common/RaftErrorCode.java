package io.github.xinfra.lab.raft.common;

import lombok.Getter;

public enum RaftErrorCode {

	UNKNOWN_ERROR(0), SUCCESS(1), NODE_NOT_FOUND(2), NODE_NOT_IN_CONF(3);

	@Getter
	private final int code;

	RaftErrorCode(int code) {
		this.code = code;
	}

}
