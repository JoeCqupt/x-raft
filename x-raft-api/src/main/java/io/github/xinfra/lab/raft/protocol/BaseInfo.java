package io.github.xinfra.lab.raft.protocol;

import lombok.Data;

import java.io.Serializable;

@Data
public abstract class BaseInfo implements Serializable {

	String requestPeerId;

	String replyPeerId;

}
