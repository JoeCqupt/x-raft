package io.github.xinfra.lab.raft;

import java.util.List;

public interface RaftServer extends LifeCycle {

	void startRaftGroup(RaftGroupOptions raftGroupOptions);

	List<RaftGroup> getRaftGroups();

}
