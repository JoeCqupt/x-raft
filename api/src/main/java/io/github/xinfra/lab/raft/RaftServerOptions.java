package io.github.xinfra.lab.raft;

import io.github.xinfra.lab.raft.transport.TransportServerOptions;
import io.github.xinfra.lab.raft.transport.TransportType;
import lombok.Data;

@Data
public class RaftServerOptions {

    TransportServerOptions transportServerOptions;

    private TransportType transportType;
}
