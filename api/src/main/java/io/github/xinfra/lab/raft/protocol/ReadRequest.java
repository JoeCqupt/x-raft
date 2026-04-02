package io.github.xinfra.lab.raft.protocol;

import lombok.Data;

@Data
public class ReadRequest extends RaftGroupAware {

    private ReadLevel readLevel;

    public static enum ReadLevel {
        DEFAULT,  LINEARIZABLE, QUORUM
    }
}
