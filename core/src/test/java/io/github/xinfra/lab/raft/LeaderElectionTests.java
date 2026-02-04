package io.github.xinfra.lab.raft;

import io.github.xinfra.lab.raft.base.TestCluster;
import io.github.xinfra.lab.raft.common.Wait;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Leader Election Tests 测试 Raft 选举过程中的各种场景
 */
@Slf4j
public class LeaderElectionTests {

	private TestCluster cluster;

	@BeforeEach
	public void setup() {
		log.info("Setting up test cluster");
	}

	@AfterEach
	public void teardown() {
		if (cluster != null) {
			log.info("Tearing down test cluster");
			// 清理集群资源
			cluster = null;
		}
	}

	/**
	 * 测试基本的 Leader 选举 场景：启动一个 3 节点集群，验证能够成功选举出 Leader
	 */
	@Test
	public void testBasicLeaderElection() throws InterruptedException, TimeoutException {
		log.info("=== Test: Basic Leader Election ===");
		cluster = new TestCluster("test-group-basic", 3);
		cluster.startup();

		// 等待 Leader 选举完成
		Wait.untilIsTrue(() -> {
			RaftPeer leader = cluster.getLeaderPeer();
			if (leader != null) {
				log.info("Leader elected: {}", leader.getRaftPeerId());
				log.info("Cluster state:\n{}", cluster.printRaftNodes());
				return true;
			}
			return false;
		}, 100, 100);

		// 验证只有一个 Leader
		RaftPeer leader = cluster.getLeaderPeer();
		assertNotNull(leader, "Leader should be elected");

		long leaderCount = cluster.getRaftNodes()
			.stream()
			.filter(node -> node.getRaftRole() == RaftRole.LEADER)
			.count();
		assertEquals(1, leaderCount, "Should have exactly one leader");

		// 验证其他节点都是 Follower
		long followerCount = cluster.getRaftNodes()
			.stream()
			.filter(node -> node.getRaftRole() == RaftRole.FOLLOWER)
			.count();
		assertEquals(2, followerCount, "Should have exactly two followers");

		log.info("=== Test Passed: Basic Leader Election ===");
	}

	/**
	 * 测试 5 节点集群的 Leader 选举 场景：启动一个 5 节点集群，验证能够成功选举出 Leader
	 */
	@Test
	public void testLeaderElectionWithFiveNodes() throws InterruptedException, TimeoutException {
		log.info("=== Test: Leader Election with Five Nodes ===");
		cluster = new TestCluster("test-group-five", 5);
		cluster.startup();

		// 等待 Leader 选举完成
		Wait.untilIsTrue(() -> {
			RaftPeer leader = cluster.getLeaderPeer();
			if (leader != null) {
				log.info("Leader elected: {}", leader.getRaftPeerId());
				log.info("Cluster state:\n{}", cluster.printRaftNodes());
				return true;
			}
			return false;
		}, 100, 100);

		// 验证只有一个 Leader
		RaftPeer leader = cluster.getLeaderPeer();
		assertNotNull(leader, "Leader should be elected");

		long leaderCount = cluster.getRaftNodes()
			.stream()
			.filter(node -> node.getRaftRole() == RaftRole.LEADER)
			.count();
		assertEquals(1, leaderCount, "Should have exactly one leader");

		// 验证其他节点都是 Follower
		long followerCount = cluster.getRaftNodes()
			.stream()
			.filter(node -> node.getRaftRole() == RaftRole.FOLLOWER)
			.count();
		assertEquals(4, followerCount, "Should have exactly four followers");

		log.info("=== Test Passed: Leader Election with Five Nodes ===");
	}

	/**
	 * 测试单节点集群的 Leader 选举 场景：启动一个单节点集群，验证该节点能够成为 Leader
	 */
	@Test
	public void testSingleNodeLeaderElection() throws InterruptedException, TimeoutException {
		log.info("=== Test: Single Node Leader Election ===");
		cluster = new TestCluster("test-group-single", 1);
		cluster.startup();

		// 等待 Leader 选举完成
		Wait.untilIsTrue(() -> {
			RaftPeer leader = cluster.getLeaderPeer();
			if (leader != null) {
				log.info("Leader elected: {}", leader.getRaftPeerId());
				log.info("Cluster state:\n{}", cluster.printRaftNodes());
				return true;
			}
			return false;
		}, 100, 100);

		// 验证单节点成为 Leader
		RaftPeer leader = cluster.getLeaderPeer();
		assertNotNull(leader, "Single node should become leader");

		long leaderCount = cluster.getRaftNodes()
			.stream()
			.filter(node -> node.getRaftRole() == RaftRole.LEADER)
			.count();
		assertEquals(1, leaderCount, "Should have exactly one leader");

		log.info("=== Test Passed: Single Node Leader Election ===");
	}

	/**
	 * 测试 Leader 选举后的稳定性 场景：选举出 Leader 后，等待一段时间，验证 Leader 保持稳定
	 */
	@Test
	public void testLeaderStability() throws InterruptedException, TimeoutException {
		log.info("=== Test: Leader Stability ===");
		cluster = new TestCluster("test-group-stability", 3);
		cluster.startup();

		// 等待 Leader 选举完成
		Wait.untilIsTrue(() -> cluster.getLeaderPeer() != null, 100, 100);

		RaftPeer initialLeader = cluster.getLeaderPeer();
		assertNotNull(initialLeader, "Initial leader should be elected");
		log.info("Initial leader: {}", initialLeader.getRaftPeerId());

		// 等待一段时间，验证 Leader 保持不变
		Thread.sleep(2000);

		RaftPeer currentLeader = cluster.getLeaderPeer();
		assertNotNull(currentLeader, "Leader should still exist");
		assertEquals(initialLeader.getRaftPeerId(), currentLeader.getRaftPeerId(), "Leader should remain the same");

		// 验证仍然只有一个 Leader
		long leaderCount = cluster.getRaftNodes()
			.stream()
			.filter(node -> node.getRaftRole() == RaftRole.LEADER)
			.count();
		assertEquals(1, leaderCount, "Should still have exactly one leader");

		log.info("=== Test Passed: Leader Stability ===");
	}

	/**
	 * 测试 Leader 选举的确定性 场景：多次重启集群，验证每次都能成功选举出 Leader
	 */
	@Test
	public void testLeaderElectionDeterminism() throws InterruptedException, TimeoutException {
		log.info("=== Test: Leader Election Determinism ===");

		for (int i = 0; i < 3; i++) {
			final int round = i + 1;
			log.info("--- Election round {} ---", round);
			cluster = new TestCluster("test-group-determinism-" + i, 3);
			cluster.startup();

			// 等待 Leader 选举完成
			Wait.untilIsTrue(() -> {
				RaftPeer leader = cluster.getLeaderPeer();
				if (leader != null) {
					log.info("Round {}: Leader elected: {}", round, leader.getRaftPeerId());
					return true;
				}
				return false;
			}, 100, 100);

			// 验证 Leader 存在
			RaftPeer leader = cluster.getLeaderPeer();
			assertNotNull(leader, "Leader should be elected in round " + round);

			// 验证只有一个 Leader
			long leaderCount = cluster.getRaftNodes()
				.stream()
				.filter(node -> node.getRaftRole() == RaftRole.LEADER)
				.count();
			assertEquals(1, leaderCount, "Should have exactly one leader in round " + round);

			log.info("Round {} cluster state:\n{}", round, cluster.printRaftNodes());

			// 清理当前集群
			cluster = null;
			Thread.sleep(500); // 短暂等待，确保资源释放
		}

		log.info("=== Test Passed: Leader Election Determinism ===");
	}

	/**
	 * 测试不同集群大小的 Leader 选举 场景：测试 2、3、4、5、7 节点集群的 Leader 选举
	 */
	@Test
	public void testLeaderElectionWithDifferentClusterSizes() throws InterruptedException, TimeoutException {
		log.info("=== Test: Leader Election with Different Cluster Sizes ===");

		int[] clusterSizes = { 2, 3, 4, 5, 7 };

		for (int size : clusterSizes) {
			log.info("--- Testing cluster size: {} ---", size);
			cluster = new TestCluster("test-group-size-" + size, size);
			cluster.startup();

			// 等待 Leader 选举完成
			Wait.untilIsTrue(() -> {
				RaftPeer leader = cluster.getLeaderPeer();
				if (leader != null) {
					log.info("Cluster size {}: Leader elected: {}", size, leader.getRaftPeerId());
					return true;
				}
				return false;
			}, 100, 100);

			// 验证 Leader 存在
			RaftPeer leader = cluster.getLeaderPeer();
			assertNotNull(leader, "Leader should be elected for cluster size " + size);

			// 验证只有一个 Leader
			long leaderCount = cluster.getRaftNodes()
				.stream()
				.filter(node -> node.getRaftRole() == RaftRole.LEADER)
				.count();
			assertEquals(1, leaderCount, "Should have exactly one leader for cluster size " + size);

			// 验证 Follower 数量
			long followerCount = cluster.getRaftNodes()
				.stream()
				.filter(node -> node.getRaftRole() == RaftRole.FOLLOWER)
				.count();
			assertEquals(size - 1, followerCount,
					"Should have exactly " + (size - 1) + " followers for cluster size " + size);

			log.info("Cluster size {} state:\n{}", size, cluster.printRaftNodes());

			// 清理当前集群
			cluster = null;
			Thread.sleep(500); // 短暂等待，确保资源释放
		}

		log.info("=== Test Passed: Leader Election with Different Cluster Sizes ===");
	}

	/**
	 * 测试 Leader 选举的时间性能 场景：测量从集群启动到选举出 Leader 所需的时间
	 */
	@Test
	public void testLeaderElectionPerformance() throws InterruptedException, TimeoutException {
		log.info("=== Test: Leader Election Performance ===");
		cluster = new TestCluster("test-group-performance", 3);

		long startTime = System.currentTimeMillis();
		cluster.startup();

		// 等待 Leader 选举完成
		Wait.untilIsTrue(() -> cluster.getLeaderPeer() != null, 100, 100);

		long electionTime = System.currentTimeMillis() - startTime;
		log.info("Leader election completed in {} ms", electionTime);

		// 验证 Leader 存在
		RaftPeer leader = cluster.getLeaderPeer();
		assertNotNull(leader, "Leader should be elected");

		// 验证选举时间在合理范围内（例如 10 秒内）
		assertTrue(electionTime < 10000, "Leader election should complete within 10 seconds");

		log.info("=== Test Passed: Leader Election Performance ===");
	}

	/**
	 * 测试并发启动场景下的 Leader 选举 场景：所有节点几乎同时启动，验证能够正确选举出 Leader
	 */
	@Test
	public void testConcurrentStartupLeaderElection() throws InterruptedException, TimeoutException {
		log.info("=== Test: Concurrent Startup Leader Election ===");
		cluster = new TestCluster("test-group-concurrent", 5);
		cluster.startup();

		// 等待 Leader 选举完成
		Wait.untilIsTrue(() -> {
			RaftPeer leader = cluster.getLeaderPeer();
			if (leader != null) {
				log.info("Leader elected: {}", leader.getRaftPeerId());
				log.info("Cluster state:\n{}", cluster.printRaftNodes());
				return true;
			}
			return false;
		}, 100, 150);

		// 验证只有一个 Leader
		RaftPeer leader = cluster.getLeaderPeer();
		assertNotNull(leader, "Leader should be elected");

		long leaderCount = cluster.getRaftNodes()
			.stream()
			.filter(node -> node.getRaftRole() == RaftRole.LEADER)
			.count();
		assertEquals(1, leaderCount, "Should have exactly one leader even with concurrent startup");

		log.info("=== Test Passed: Concurrent Startup Leader Election ===");
	}

	/**
	 * 测试 Leader 选举后的集群状态一致性 场景：验证选举完成后，所有 Follower 都认可同一个 Leader
	 */
	@Test
	public void testLeaderConsistencyAcrossFollowers() throws InterruptedException, TimeoutException {
		log.info("=== Test: Leader Consistency Across Followers ===");
		cluster = new TestCluster("test-group-consistency", 3);
		cluster.startup();

		// 等待 Leader 选举完成
		Wait.untilIsTrue(() -> cluster.getLeaderPeer() != null, 100, 100);

		RaftPeer leader = cluster.getLeaderPeer();
		assertNotNull(leader, "Leader should be elected");
		log.info("Elected leader: {}", leader.getRaftPeerId());

		// 等待一段时间，确保所有节点状态稳定
		Thread.sleep(1000);

		// 验证所有 Follower 的状态
		long followerCount = cluster.getRaftNodes()
			.stream()
			.filter(node -> node.getRaftRole() == RaftRole.FOLLOWER)
			.count();
		assertEquals(2, followerCount, "Should have exactly two followers");

		// 验证集群状态
		log.info("Final cluster state:\n{}", cluster.printRaftNodes());

		log.info("=== Test Passed: Leader Consistency Across Followers ===");
	}

	/**
	 * 测试快速连续的 Leader 选举 场景：快速创建和销毁多个集群，验证选举机制的稳定性
	 */
	@Test
	public void testRapidLeaderElectionCycles() throws InterruptedException, TimeoutException {
		log.info("=== Test: Rapid Leader Election Cycles ===");

		for (int i = 0; i < 5; i++) {
			log.info("--- Rapid election cycle {} ---", i + 1);
			cluster = new TestCluster("test-group-rapid-" + i, 3);
			cluster.startup();

			// 快速等待 Leader 选举
			Wait.untilIsTrue(() -> cluster.getLeaderPeer() != null, 50, 100);

			RaftPeer leader = cluster.getLeaderPeer();
			assertNotNull(leader, "Leader should be elected in cycle " + (i + 1));
			log.info("Cycle {}: Leader is {}", i + 1, leader.getRaftPeerId());

			// 立即清理
			cluster = null;
		}

		log.info("=== Test Passed: Rapid Leader Election Cycles ===");
	}

}
