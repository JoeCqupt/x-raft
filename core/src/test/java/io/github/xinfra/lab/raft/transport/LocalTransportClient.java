package io.github.xinfra.lab.raft.transport;

import io.github.xinfra.lab.raft.AbstractLifeCycle;
import io.github.xinfra.lab.raft.RaftNode;
import io.github.xinfra.lab.raft.core.transport.RaftApi;
import io.github.xinfra.lab.raft.protocol.ErrorInfo;
import io.github.xinfra.lab.raft.protocol.VoteRequest;
import io.github.xinfra.lab.raft.protocol.VoteResponse;

import java.net.SocketAddress;
import java.util.List;
import java.util.Optional;

import static io.github.xinfra.lab.raft.common.RaftError.NODE_NOT_FOUND;

public class LocalTransportClient extends AbstractLifeCycle implements TransportClient {

	private TransportClientOptions transportClientOptions;

	private List<RaftNode> raftNodes;

	public LocalTransportClient(TransportClientOptions transportClientOptions) {
		this.transportClientOptions = transportClientOptions;
	}

	@Override
	public void connect(SocketAddress socketAddress) {

	}

	@Override
	public void reconnect(SocketAddress socketAddress) {

	}

	@Override
	public void disconnect(SocketAddress socketAddress) {

	}

	@Override
	public <T, R> R blockingCall(RequestApi requestApi, SocketAddress socketAddress, T request,
			CallOptions callOptions) {
		if (requestApi == RaftApi.requestVote) {
			VoteRequest voteRequest = (VoteRequest) request;
			String requestRaftGroupId = voteRequest.getRaftGroupId();
			String requestPeerId = voteRequest.getRequestPeerId();
			Optional<RaftNode> raftOpt = raftNodes.stream()
				.filter(raftNode -> raftNode.raftGroupId().equals(requestRaftGroupId)
						&& raftNode.raftPeerId().getPeerId().equals(requestPeerId))
				.findFirst();
			if (raftOpt.isPresent()) {
				return (R) raftOpt.get().requestVote(voteRequest);
			}
			else {
				// todo:
				VoteResponse voteResponse = new VoteResponse();
				voteResponse.setSuccess(false);
				voteResponse.setErrorInfo(new ErrorInfo(NODE_NOT_FOUND.getCode(),
						"node not found:" + requestRaftGroupId + "@" + requestPeerId));
				return (R) voteResponse;

			}
		}
		// todo:
		return null;
	}

	public void setRaftNodes(List<RaftNode> raftNodes) {
		this.raftNodes = raftNodes;
	}

}
