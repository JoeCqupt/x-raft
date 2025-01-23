package io.github.xinfra.lab.raft;

import lombok.Getter;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class RaftNodeState {

    @Getter
    private final AtomicLong currentTerm = new AtomicLong();

    private volatile RaftPeer leaderId;

    private volatile RaftPeer votedFor;

    @Getter
    private volatile RaftRole role;

    private final XRaftNode xRaftNode;


    private final RaftConfigurationState raftConfigurationState;

    private final AtomicReference<FollowerState> followerState = new AtomicReference<>();

    private final AtomicReference<CandidateState> candidateState = new AtomicReference<>();

    private final AtomicReference<LeaderState> leaderState = new AtomicReference<>();

    public RaftNodeState(XRaftNode xRaftNode) {
        this.xRaftNode = xRaftNode;
        RaftConfiguration initialConfiguration = new RaftConfiguration(xRaftNode.self(),null,
                new PeerConfiguration(xRaftNode.getRaftGroup().getRaftPeers(),
                        xRaftNode.getRaftGroup().getLearners()));
        this.raftConfigurationState = new RaftConfigurationState(initialConfiguration);
    }

    public void startFollowerState() {
        role = RaftRole.FOLLOWER;
        followerState.updateAndGet(current -> current == null ? new FollowerState(xRaftNode) : current).start();
    }

    public void shutdownFollowerState() {
        FollowerState follower = followerState.getAndSet(null);
        if (follower != null) {
            follower.shutdown();
            follower.interrupt();
        }
    }

    public void startCandidateState() {
        role = RaftRole.CANDIDATE;
        candidateState.updateAndGet(current -> current == null ? new CandidateState(xRaftNode) : current).start();
    }

    public void shutdownCandidateState() {
        CandidateState candidate = candidateState.getAndSet(null);
        if (candidate != null) {
            candidate.shutdown();
            candidate.interrupt();
        }
    }

    public void startLeaderState() {
        role = RaftRole.LEADER;
        leaderState.updateAndGet(current -> current == null ? new LeaderState(xRaftNode) : current).start();
    }

    public void shutdownLeaderState() {
    }

    public RaftConfiguration getRaftConfiguration() {
        return raftConfigurationState.getCurrentConfiguration();
    }

}
