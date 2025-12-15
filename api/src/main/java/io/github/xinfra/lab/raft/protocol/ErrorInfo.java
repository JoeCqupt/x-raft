package io.github.xinfra.lab.raft.protocol;

import lombok.Data;

@Data
public class ErrorInfo {

	int errorCode;

	String errorMsg;

	public ErrorInfo(int errorCode, String errorMsg) {
		this.errorCode = errorCode;
		this.errorMsg = errorMsg;
	}

}
