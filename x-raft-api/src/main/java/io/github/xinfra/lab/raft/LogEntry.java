package io.github.xinfra.lab.raft;

public interface LogEntry {

	long index();

	long term();

}
