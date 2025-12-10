package io.github.xinfra.lab.raft;

import lombok.Data;

import java.net.InetSocketAddress;
import java.util.Objects;

@Data
public class RaftPeer {

	private String raftPeerId;

	private InetSocketAddress address;

	private RaftRole role = RaftRole.FOLLOWER;
}
