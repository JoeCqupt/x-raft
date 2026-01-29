package io.github.xinfra.lab.raft.protocol;

import lombok.Data;

import java.io.Serializable;

@Data
public class VoteResponse implements Serializable {

	Long term;

	boolean voteGranted;

}
