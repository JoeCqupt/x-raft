package io.github.xinfra.lab.raft;

import java.util.List;

public interface RaftServer extends LifeCycle {

	RaftGroup startRaftGroup(RaftGroupOptions raftGroupOptions);

	List<RaftGroup> getRaftGroups();

}
