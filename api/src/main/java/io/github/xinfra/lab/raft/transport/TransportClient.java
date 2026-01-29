package io.github.xinfra.lab.raft.transport;

import io.github.xinfra.lab.raft.LifeCycle;

import java.net.SocketAddress;

public interface TransportClient extends LifeCycle {

	void connect(SocketAddress socketAddress) throws Exception;

	void reconnect(SocketAddress socketAddress) throws Exception;

	void disconnect(SocketAddress socketAddress) throws Exception;

	<T, R> void asyncCall(RequestApi requestApi, T request, SocketAddress socketAddress, CallOptions callOptions,
			ResponseCallBack<R> callBack) throws Exception;

}
