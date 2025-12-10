package io.github.xinfra.lab.raft;

import lombok.Data;

import java.net.InetSocketAddress;

@Data
public class RaftPeer {

	private String raftPeerId;

	private InetSocketAddress address;

	private RaftRole initRole = RaftRole.FOLLOWER;

	private int priority = 1; //todo: default priority
}
