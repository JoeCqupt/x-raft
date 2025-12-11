package io.github.xinfra.lab.raft.protocol;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class VoteResponse extends BaseInfo implements Serializable {

	Long term;

	boolean voteGranted;

	boolean shouldShutdown;

}
