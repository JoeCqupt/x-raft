package io.github.xinfra.lab.raft.transport;

public interface ResponseCallBack<R> {

	void onResponse(R response);

	void onException(Throwable throwable);

}
