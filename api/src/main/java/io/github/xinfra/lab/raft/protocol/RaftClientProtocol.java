package io.github.xinfra.lab.raft.protocol;

public interface RaftClientProtocol {

	void applyOperation(Operation operation);

}
