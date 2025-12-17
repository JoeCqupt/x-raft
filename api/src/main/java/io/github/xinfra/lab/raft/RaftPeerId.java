package io.github.xinfra.lab.raft;

import lombok.Data;

import java.net.InetSocketAddress;

@Data
public class RaftPeerId {

	private String peerId;

	private InetSocketAddress address;


}
