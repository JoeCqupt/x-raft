package io.github.xinfra.lab.raft;


import lombok.Getter;

public class RaftServer extends AbstractLifeCycle implements RaftNode, RaftServerProtocol {
    @Getter
    private RaftPeer raftPeer;
    @Getter
    private RaftGroup raftGroup;
    @Getter
    private RaftServerConfig config;
    @Getter
    private RaftNodeState state;

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
    public synchronized void startup() {
        super.startup();
        state.startFollowerState();
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

    public synchronized void changeToCandidate() {
        state.shutdownFollowerState();
        state.startCandidateState();
    }

    public synchronized  void changeToLeader() {
    }
}
