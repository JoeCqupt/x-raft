package io.github.xinfra.lab.raft;

import io.github.xinfra.lab.raft.base.TestCluster;
import io.github.xinfra.lab.raft.common.Wait;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeoutException;

@Slf4j
public class BaseUnitTest {

	static TestCluster cluster;

	@BeforeAll
	public static void setupCluster() {
		cluster = new TestCluster("test-raft-group", 3);
		cluster.startup();
	}

	@Test
	public void testLeaderElection() throws InterruptedException, TimeoutException {

		Wait.untilIsTrue(() -> {
			RaftPeerId leader = cluster.getLeaderPeer();
			if (leader != null) {
				log.info("cluster:{}", cluster.printRaftNodes());
				return true;
			}
			return false;
		}, 300, 10);

	}

}
