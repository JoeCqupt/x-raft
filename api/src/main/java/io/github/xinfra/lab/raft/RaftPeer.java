package io.github.xinfra.lab.raft;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.net.InetSocketAddress;

@Data
public class RaftPeer {

	@EqualsAndHashCode.Include
	private String raftPeerId;

	@EqualsAndHashCode.Include
	private InetSocketAddress address;

}
