package io.github.xinfra.lab.raft;

import lombok.Data;
import lombok.ToString;

import java.net.InetSocketAddress;

@Data
@ToString
public class RaftPeer {

	private String raftPeerId;

	private InetSocketAddress address;

}
