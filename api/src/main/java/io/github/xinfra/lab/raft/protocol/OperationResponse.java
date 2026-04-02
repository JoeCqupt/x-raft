package io.github.xinfra.lab.raft.protocol;

import lombok.Data;

@Data
public class OperationResponse {
    boolean success;
    String errorMsg;
    String redirect;

    byte[] data;
}
