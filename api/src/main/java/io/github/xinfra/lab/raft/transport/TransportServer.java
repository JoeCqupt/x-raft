package io.github.xinfra.lab.raft.transport;


import io.github.xinfra.lab.raft.LifeCycle;

public interface TransportServer extends LifeCycle {

    void registerRequestHandler(RequestApi requestApi, RequestHandler requestHandler);

}
