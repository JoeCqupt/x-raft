package io.github.xinfra.lab.raft.transport;

public interface RequestHandler<T, R> {

	R handle(T request);

}
