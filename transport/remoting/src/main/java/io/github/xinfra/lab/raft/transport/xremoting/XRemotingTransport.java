package io.github.xinfra.lab.raft.transport.xremoting;


import io.github.xinfra.lab.raft.transport.TransportClient;
import io.github.xinfra.lab.raft.transport.TransportClientOptions;
import io.github.xinfra.lab.raft.transport.TransportServer;
import io.github.xinfra.lab.raft.transport.TransportServerOptions;
import io.github.xinfra.lab.raft.transport.TransportType;


public enum XRemotingTransport  implements TransportType {

	remoting;



	@Override
	public TransportClient newClient(TransportClientOptions transportClientOptions) {
		return null;
	}

	@Override
	public TransportServer newServer(TransportServerOptions transportServerOptions) {
		return null;
	}
}
