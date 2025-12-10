package io.github.xinfra.lab.raft;

import io.github.xinfra.lab.raft.base.LocalXRaftCluster;
import io.github.xinfra.lab.raft.common.Wait;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeoutException;

@Slf4j
public class BaseUnitTest {
    static LocalXRaftCluster cluster;

    @BeforeAll
    public static void setupCluster() {
        cluster = new LocalXRaftCluster("test-group", 3);
        cluster.startup();
    }

    @Test
    public void testLeaderElection() throws InterruptedException, TimeoutException {

        Wait.untilIsTrue(() -> {
            RaftPeer leader = cluster.getLeaderPeer();
            if (leader != null) {
                log.info("cluster: {}", cluster);
                return true;
            }
            return false;
        }, 30, 100);

    }

}
