package io.github.xinfra.lab.raft;

import lombok.Getter;

import java.util.concurrent.atomic.AtomicReference;

public class RaftNodeState {

    @Getter
    private volatile RaftRole role;
    private final RaftServer raftServer;
    private final AtomicReference<FollowerState> followerState = new AtomicReference<>();
    private final AtomicReference<CandidateState> candidateState = new AtomicReference<>();

    public RaftNodeState(RaftServer raftServer) {
        this.raftServer = raftServer;
    }

    public void startFollowerState() {
        role = RaftRole.FOLLOWER;
        followerState.updateAndGet(current -> current == null ? new FollowerState(raftServer) : current)
                .start();
    }

    public void shutdownFollowerState() {
        // todo
    }

    public void startCandidateState() {
        role = RaftRole.CANDIDATE;
        candidateState.updateAndGet(current -> current == null ? new CandidateState(raftServer) : current)
                .start();
    }
}
