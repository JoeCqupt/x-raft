package io.github.xinfra.lab.raft;

import io.github.xinfra.lab.raft.base.TestCluster;
import io.github.xinfra.lab.raft.common.Wait;
import io.github.xinfra.lab.raft.core.XRaftNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeoutException;

@Slf4j
public class BaseUnitTest {

	@Test
	public void testLeaderElection() throws InterruptedException, TimeoutException {

		TestCluster cluster = new TestCluster("test-raft-group", 3);
		cluster.startup();

		Wait.untilIsTrue(() -> {
			XRaftNode leader = cluster.getLeaderNode();
			if (leader != null) {
				log.info("cluster:{}", cluster.printRaftNodes());
				return true;
			}
			return false;
		}, 100, 100);

		cluster.shutdown();
	}

}
