package io.github.xinfra.lab.raft.transport;

public enum LocalTransportType implements TransportType {

	local;

	@Override
	public TransportClient newClient(TransportClientOptions transportClientOptions) {
		return new LocalTransportClient(transportClientOptions);
	}

	@Override
	public TransportServer newServer(TransportServerOptions transportServerOptions) {
		return new LocalTransportServer(transportServerOptions);
	}

}
