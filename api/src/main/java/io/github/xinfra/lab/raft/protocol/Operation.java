package io.github.xinfra.lab.raft.protocol;

public interface Operation<T> {

    byte[] serialize();

    T deserialize(byte[] data);

}
