package io.github.xinfra.lab.raft;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class VoteResponse extends TransportInfo implements Serializable {

	int term;

	boolean voteGranted;

	boolean shouldShutdown;

}
