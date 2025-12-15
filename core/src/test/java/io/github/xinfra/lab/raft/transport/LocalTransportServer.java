package io.github.xinfra.lab.raft.transport;

import io.github.xinfra.lab.raft.AbstractLifeCycle;

public class LocalTransportServer extends AbstractLifeCycle implements TransportServer {

	private TransportServerOptions transportServerOptions;

	public LocalTransportServer(TransportServerOptions transportServerOptions) {
		this.transportServerOptions = transportServerOptions;
	}

	@Override
	public void registerRequestHandler(RequestApi requestApi, RequestHandler requestHandler) {

	}

}
