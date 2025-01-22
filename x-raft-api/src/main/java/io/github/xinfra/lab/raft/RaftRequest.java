package io.github.xinfra.lab.raft;

import lombok.Data;

import java.io.Serializable;

@Data
public abstract class RaftRequest implements Serializable {

	String requestPeerId;

	String replyPeerId;

}
