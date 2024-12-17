package io.github.xinfra.lab.raft;


import lombok.Getter;

import java.util.List;

public class RaftServer extends AbstractLifeCycle implements RaftNode, RaftServerProtocol {
    private RaftPeer raftPeer;
    private RaftGroup raftGroup;
    @Getter
    private RaftServerConfig config;
    @Getter
    private RaftNodeState state;
    @Getter
    private RaftLog raftLog;

    @Getter
    private RaftServerRpc raftServerRpc;

    public RaftServer(RaftPeer raftPeer, RaftGroup raftGroup, RaftServerConfig config) {
        this.raftPeer = raftPeer;
        this.raftGroup = raftGroup;
        this.config = config;
        state = new RaftNodeState(this);
    }

    @Override
    public RaftPeer getPeer() {
        return raftPeer;
    }

    @Override
    public RaftGroup getGroup() {
        return raftGroup;
    }

    @Override
    public void startup() {
        super.startup();
        changeToFollower();
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    @Override
    public RequestVoteResponse requestVote(RequestVoteRequest requestVoteRequest) {
        // todo
        return null;
    }

    public Long getRandomElectionTimeout() {
        // todo
        return null;
    }

    private synchronized void changeToFollower() {
        if (state.getRole() == RaftRole.CANDIDATE){
            state.shutdownCandidateState();
        }if (state.getRole() == RaftRole.LEADER){
            state.shutdownLeaderState();
        }
        state.startFollowerState();
    }

    public synchronized void changeToCandidate() {
        state.shutdownFollowerState();
        state.startCandidateState();
    }

    public synchronized  void changeToLeader() {
        state.shutdownCandidateState();
        state.startLeaderState();
    }
}
