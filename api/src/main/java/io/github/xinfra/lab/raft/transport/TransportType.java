package io.github.xinfra.lab.raft.transport;

public interface TransportType {

    TransportClient newClient(TransportClientOptions transportClientOptions);

    TransportServer newServer(TransportServerOptions transportServerOptions);
}
