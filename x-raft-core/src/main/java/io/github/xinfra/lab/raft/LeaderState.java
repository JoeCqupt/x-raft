package io.github.xinfra.lab.raft;

public class LeaderState extends Thread {
    private final RaftServer raftServer;
    public LeaderState(RaftServer raftServer) {
        this.raftServer = raftServer;
    }

    @Override
    public void run() {
        // todo
    }
}
