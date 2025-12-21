package io.github.xinfra.lab.raft.protocol;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Data
public class VoteResponse extends ResponseMessage implements Serializable {

	Long term;

	boolean voteGranted;

}
