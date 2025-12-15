package io.github.xinfra.lab.raft.common;

import lombok.Getter;

public enum RaftError {

	UNKNOWN_ERROR(0), SUCCESS(1), NODE_NOT_FOUND(2);

	@Getter
	int code;

	RaftError(int code) {
		this.code = code;
	}

}
