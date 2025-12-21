package io.github.xinfra.lab.raft.protocol;

import lombok.Data;

import java.io.Serializable;

@Data
public abstract class ResponseMessage implements Serializable {

	boolean success;

	/**
	 * only used when success is false
	 */
	ErrorInfo errorInfo;

}
