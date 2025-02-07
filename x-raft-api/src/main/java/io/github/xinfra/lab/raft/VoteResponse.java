package io.github.xinfra.lab.raft;

import lombok.Data;

import java.io.Serializable;

@Data
public class VoteResponse extends TransportInfo implements Serializable {

	int term;

	boolean voteGranted;

	boolean shouldShutdown;

}
