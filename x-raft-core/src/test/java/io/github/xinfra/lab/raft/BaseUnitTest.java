package io.github.xinfra.lab.raft;

import com.google.common.collect.Sets;
import io.github.xinfra.lab.raft.base.LocalXRaftNode;
import io.github.xinfra.lab.raft.common.Wait;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeoutException;

public class BaseUnitTest {

	static LocalXRaftNode node1;
	static LocalXRaftNode node2;
	static LocalXRaftNode node3;

	@BeforeAll
	public static void setupCluster() {
		RaftPeer raftPeer1 = new RaftPeer();
		raftPeer1.setRaftPeerId("node1");
		raftPeer1.setAddress(new InetSocketAddress("localhost", 6665));

		RaftPeer raftPeer2 = new RaftPeer();
		raftPeer2.setRaftPeerId("node2");
		raftPeer2.setAddress(new InetSocketAddress("localhost", 6666));

		RaftPeer raftPeer3 = new RaftPeer();
		raftPeer3.setRaftPeerId("node3");
		raftPeer3.setAddress(new InetSocketAddress("localhost", 6667));

		RaftGroup raftGroup = new RaftGroup("raft-group", Sets.newHashSet(raftPeer1, raftPeer2, raftPeer3));

		node1 = new LocalXRaftNode(raftPeer1, raftGroup);
		node2 = new LocalXRaftNode(raftPeer2, raftGroup);
		node3 = new LocalXRaftNode(raftPeer3, raftGroup);

		node1.addRaftPeerNode(node2, node3);

		node2.addRaftPeerNode(node1, node3);

		node3.addRaftPeerNode(node1, node2);

		node1.startup();
		node2.startup();
		node3.startup();
	}

	@Test
	public void testLeaderElection() throws InterruptedException, TimeoutException {

		Wait.untilIsTrue(() -> {
			if (node1.getState().getRole() == RaftRole.LEADER) {
				System.out.println("node1 is leader");
				return true;
			}
			if (node2.getState().getRole() == RaftRole.LEADER) {
				System.out.println("node2 is leader");
				return true;
			}
			if (node3.getState().getRole() == RaftRole.LEADER) {
				System.out.println("node3 is leader");
				return true;
			}
			return false;
		}, 30, 100);


	}

}
