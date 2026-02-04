package io.github.xinfra.lab.raft.statemachine;

public class NOOPStateMachine implements StateMachine {

	public static final NOOPStateMachine INSTANCE = new NOOPStateMachine();

	@Override
	public void onLeaderChange(String leaderId) {

	}

}
