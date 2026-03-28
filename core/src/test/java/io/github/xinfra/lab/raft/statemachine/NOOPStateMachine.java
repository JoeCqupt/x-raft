package io.github.xinfra.lab.raft.statemachine;

import io.github.xinfra.lab.raft.AbstractLifeCycle;

public class NOOPStateMachine extends AbstractLifeCycle implements StateMachine {

	@Override
	public void onLeaderChange(String leaderId) {

	}

}
