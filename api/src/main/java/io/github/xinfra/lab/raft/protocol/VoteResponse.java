package io.github.xinfra.lab.raft.protocol;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@Data
@ToString
public class VoteResponse implements Serializable {

	Long term;

	boolean voteGranted;

}
