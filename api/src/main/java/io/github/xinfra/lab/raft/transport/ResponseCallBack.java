package io.github.xinfra.lab.raft.transport;

public interface ResponseCallBack<R> {

    public void onResponse(R response);

    public void onError(Throwable throwable);

}
