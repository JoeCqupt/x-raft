package io.github.xinfra.lab.raft.statemachine;

public interface StateMachine {

	void onLeaderChange(String leaderId);

}
