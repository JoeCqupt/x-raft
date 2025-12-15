package io.github.xinfra.lab.raft.transport;

import io.github.xinfra.lab.raft.LifeCycle;

import java.net.SocketAddress;

public interface TransportClient extends LifeCycle {

	void connect(SocketAddress socketAddress);

	void reconnect(SocketAddress socketAddress);

	void disconnect(SocketAddress socketAddress);

	<T, R> R blockingCall(RequestApi requestApi, SocketAddress socketAddress, T request, CallOptions callOptions);

}
