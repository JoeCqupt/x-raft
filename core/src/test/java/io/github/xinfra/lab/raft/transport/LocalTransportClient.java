package io.github.xinfra.lab.raft.transport;

import io.github.xinfra.lab.raft.AbstractLifeCycle;
import io.github.xinfra.lab.raft.RaftNode;
import io.github.xinfra.lab.raft.exception.RaftException;
import io.github.xinfra.lab.raft.protocol.AppendEntriesRequest;
import io.github.xinfra.lab.raft.protocol.AppendEntriesResponse;
import io.github.xinfra.lab.raft.protocol.RaftGroupAware;
import io.github.xinfra.lab.raft.protocol.VoteRequest;
import io.github.xinfra.lab.raft.protocol.VoteResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;

import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import static io.github.xinfra.lab.raft.common.RaftErrorCode.NODE_NOT_FOUND;
import static io.github.xinfra.lab.raft.core.transport.RaftApi.appendEntries;
import static io.github.xinfra.lab.raft.core.transport.RaftApi.requestVote;

@Slf4j
public class LocalTransportClient extends AbstractLifeCycle implements TransportClient {

	private TransportClientOptions transportClientOptions;

	private List<RaftNode> raftNodes;

	private AtomicLong requestIdGenerator = new AtomicLong();

	private Map<Long, ResponseCallBack> responseCallBackMap = new ConcurrentHashMap<>();

	ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);

    ExecutorService callExecutor = Executors.newCachedThreadPool();

	public void setRaftNodes(List<RaftNode> raftNodes) {
		this.raftNodes = raftNodes;
	}

	public LocalTransportClient(TransportClientOptions transportClientOptions) {
		this.transportClientOptions = transportClientOptions;
	}

	@Override
	public void shutdown() {
		super.shutdown();
		executor.shutdown();
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
	public <T, R> void asyncCall(RequestApi requestApi, T request, SocketAddress socketAddress, CallOptions callOptions,
			ResponseCallBack<R> callBack) throws Exception {
		log.info("asyncCall request: {} {} {}", requestApi, request, callOptions);
		long requestId = requestIdGenerator.incrementAndGet();
		responseCallBackMap.put(requestId, callBack);

		// timeout task
		ScheduledFuture<?> timeout = executor.schedule(() -> {
			exception(requestId, new TimeoutException("timeout"));
		}, callOptions.getTimeoutMs(), TimeUnit.MILLISECONDS);

		callExecutor.execute(
                () -> simulateCall(requestId, requestApi, request, timeout)
        );
	}

    public void simulateCall(long requestId, RequestApi requestApi, Object request, Future<?> timeout) {
        try {
            // random delay
			 TimeUnit.MILLISECONDS.sleep(RandomUtils.nextLong(10, 30));

            RaftGroupAware raftGroupAware = (RaftGroupAware) request;
            String requestRaftGroupId = raftGroupAware.getRaftGroupId();
            String requestPeerId = raftGroupAware.getRaftPeerId();
            RaftNode raftNode = raftNodes.stream()
                    .filter(node -> node.getRaftGroupId().equals(requestRaftGroupId)
                            && node.getRaftPeer().getRaftPeerId().equals(requestPeerId))
                    .findFirst()
                    .get();
            if (raftNode == null) {
                throw new RaftException(NODE_NOT_FOUND);
            }

            Object response = null;
            if (requestApi == requestVote) {
                VoteRequest voteRequest = (VoteRequest) request;
                VoteResponse voteResponse;
                if (voteRequest.isPreVote()) {
                    voteResponse = raftNode.handlePreVoteRequest(voteRequest);
                }
                else {
                    voteResponse = raftNode.handleVoteRequest(voteRequest);
                }
                response = voteResponse;

            }
            else if (requestApi == appendEntries) {
                AppendEntriesResponse appendEntriesResponse = raftNode
                        .handleAppendEntries((AppendEntriesRequest) request);
                response = appendEntriesResponse;
            }
            else {
                throw new IllegalStateException("not support api: " + requestApi);
            }
            timeout.cancel(true);
            response(requestId, response);
        }
        catch (Exception e) {
            log.error("asyncCall ex", e);
            timeout.cancel(true);
            exception(requestId, e);
        }
	}

	public void response(long requestId, Object response) {
		log.info("asyncCall response: {}", response);
		ResponseCallBack responseCallBack = responseCallBackMap.remove(requestId);
		if (responseCallBack != null) {
			responseCallBack.onResponse(response);
		}
	}

	public void exception(long requestId, Throwable throwable) {
		log.error("asyncCall exception: {}", throwable.getMessage());
		ResponseCallBack responseCallBack = responseCallBackMap.remove(requestId);
		if (responseCallBack != null) {
			responseCallBack.onException(throwable);
		}
	}

}
