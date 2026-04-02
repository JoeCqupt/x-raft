package io.github.xinfra.lab.raft.protocol;

public interface RaftClientService {

	OperationResponse applyOperation(OperationRequest operationRequest);

    ReadResponse read(ReadRequest readRequest);
}
