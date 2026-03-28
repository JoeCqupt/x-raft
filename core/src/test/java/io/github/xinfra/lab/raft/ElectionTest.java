package io.github.xinfra.lab.raft;

import io.github.xinfra.lab.raft.base.TestCluster;
import io.github.xinfra.lab.raft.common.Wait;
import io.github.xinfra.lab.raft.core.XRaftNode;
import io.github.xinfra.lab.raft.core.XRaftServer;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * election tests
 */
public class ElectionTest extends BaseUnitTest {

	private static final Logger logger = LoggerFactory.getLogger(ElectionTest.class);

	@Test
	public void testThreeNodeElection() throws Exception {
		logger.info("start testThreeNodeElection");

		TestCluster cluster = new TestCluster("test-group", 3);
		try {
			cluster.startup();
			logger.info("cluster startup completed");

			Wait.untilIsTrue(() -> {
				XRaftNode leader = cluster.getLeaderNode();
				if (leader != null) {
					logger.info("election success, Leader node: {}", leader.getRaftPeer());
					return true;
				}
				return false;
			}, 500, 20);

			XRaftNode leader = cluster.getLeaderNode();
			assertNotNull(leader, "leader should not be null");

			assertEquals(RaftRole.LEADER, leader.getRaftRole(), "leader role should be LEADER");

			List<XRaftNode> allNodes = cluster.getRaftNodes();
			long leaderCount = allNodes.stream().filter(node -> node.getRaftRole() == RaftRole.LEADER).count();
			assertEquals(1, leaderCount, "there should be only one leader in the cluster");

			long followerCount = allNodes.stream().filter(node -> node.getRaftRole() == RaftRole.FOLLOWER).count();
			assertEquals(2, followerCount, "there should be 2 followers in the cluster");

			long leaderTerm = leader.getState().getCurrentTerm();
			for (XRaftNode node : allNodes) {
				assertEquals(leaderTerm, node.getState().getCurrentTerm(), "all nodes' term should be consistent");
			}

			logger.info("3 node election test passed");

		}
		finally {
			cluster.shutdown();
			logger.info("cluster shutdown completed");
		}
	}

	/**
	 * three node election test - leader failure
	 */
	@Test
	public void testThreeNodeElectionWithLeaderFailure() throws Exception {
		logger.info("start testThreeNodeElectionWithLeaderFailure");

		TestCluster cluster = new TestCluster("test-group", 3);
		try {
			cluster.startup();
			logger.info("cluster startup completed");

			Wait.untilIsTrue(() -> {
				XRaftNode leader = cluster.getLeaderNode();
				if (leader != null) {
					logger.info("first election success, Leader node: {}", leader.getRaftPeer());
					return true;
				}
				return false;
			}, 500, 20);

			XRaftNode firstLeader = cluster.getLeaderNode();
			assertNotNull(firstLeader, "first leader should not be null");

			String firstLeaderId = firstLeader.getRaftGroupPeerId();
			long firstTerm = firstLeader.getState().getCurrentTerm();
			logger.info("first leader: {}, term: {}", firstLeaderId, firstTerm);

			cluster.getRaftNodeRaftServerMap().get(firstLeader).shutdown();
			logger.info("leader node {} has been shutdown", firstLeaderId);

			Wait.untilIsTrue(() -> {
				XRaftNode newLeader = cluster.getLeaderNode();
				if (newLeader != null && !newLeader.getRaftGroupPeerId().equals(firstLeaderId)) {
					logger.info("re-election success，new leader: {}, term: {}", newLeader.getRaftPeer(),
							newLeader.getState().getCurrentTerm());
					return true;
				}
				return false;
			}, 500, 20);

			XRaftNode newLeader = cluster.getLeaderNode();
			assertNotNull(newLeader, "new leader should not be null");
			assertNotEquals(firstLeaderId, newLeader.getRaftGroupPeerId(),
					"new leader should not be the same as the old leader");

			// verify the new leader's role
			assertEquals(RaftRole.LEADER, newLeader.getRaftRole(), "new leader's role should be LEADER");

			// verify the new leader's term should be greater than the old term
			assertTrue(newLeader.getState().getCurrentTerm() > firstTerm,
					"new leader's term should be greater than the old term");

			// verify there is only one leader in the remaining validate nodes
			List<XRaftNode> remainingNodes = cluster.getRaftNodes()
				.stream()
				.filter(node -> node.isStarted())
				.collect(Collectors.toList());
			long leaderCount = remainingNodes.stream().filter(node -> node.getRaftRole() == RaftRole.LEADER).count();
			assertEquals(1, leaderCount, "there should be only one leader in the remaining validate nodes");

			// verify another node is Follower
			long followerCount = remainingNodes.stream()
				.filter(node -> node.getRaftRole() == RaftRole.FOLLOWER)
				.count();
			assertEquals(1, followerCount, "there should be 1 follower in the remaining validate nodes");

			// verify the term of remaining nodes are consistent
			long newTerm = newLeader.getState().getCurrentTerm();
			for (XRaftNode node : remainingNodes) {
				assertEquals(newTerm, node.getState().getCurrentTerm(),
						"the term of remaining nodes should be consistent");
			}

			logger.info("3 node election - leader failure test passed");
		}
		finally {
			// close cluster
			cluster.getRaftServers().stream().filter(XRaftServer::isStarted).forEach(XRaftServer::shutdown);
			logger.info("cluster has been closed");
		}
	}

}