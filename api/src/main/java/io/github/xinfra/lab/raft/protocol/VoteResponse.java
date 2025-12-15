package io.github.xinfra.lab.raft.protocol;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class VoteResponse extends Message implements Serializable {

	Long term;

	boolean voteGranted;

	// todo
	boolean shouldShutdown;

}
