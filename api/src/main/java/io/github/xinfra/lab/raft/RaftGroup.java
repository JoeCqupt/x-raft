package io.github.xinfra.lab.raft;

public interface RaftGroup extends LifeCycle {

	String getRaftGroupId();

	RaftNode getRaftNode();

}
