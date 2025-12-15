package io.github.xinfra.lab.raft.transport;

import lombok.Data;

@Data
public class CallOptions {

	private long timeoutMs = 3000;

}
