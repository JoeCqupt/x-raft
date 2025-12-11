package io.github.xinfra.lab.raft.log;

public interface LogEntry {

	Long index();

	Long term();

}
