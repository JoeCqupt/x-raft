package io.github.xinfra.lab.raft;

public interface LifeCycle {

	void startup();

	void shutdown();

	boolean isStarted();

}
