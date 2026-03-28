package io.github.xinfra.lab.raft.statemachine;

import io.github.xinfra.lab.raft.LifeCycle;

public interface StateMachine extends LifeCycle {

	void onLeaderChange(String leaderId);

}
