package io.github.xinfra.lab.raft.protocol;

import lombok.Data;

@Data
public class OperationRequest extends RaftGroupAware {

    private byte[] data;

}
