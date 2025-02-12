package io.github.xinfra.lab.raft.log;

public interface LogEntry {

	long index();

	long term();

}
