package io.github.xinfra.lab.raft;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;


@Slf4j
public class CandidateState extends Thread {
    private volatile boolean running = true;
    private final RaftServer raftServer;

    public CandidateState(RaftServer raftServer) {
        this.raftServer = raftServer;
    }

    @Override
    public void run() {
        while (shouldRun()) {
            try {
                if (preVote()) {
                    if (vote()) {
                        raftServer.changeToLeader();
                    }
                }
            } catch (Throwable t) {
                log.error("CandidateState error", t);
            }
        }
    }

    private boolean vote() {
        // todo
        return false;
    }

    private boolean preVote() {
        synchronized (raftServer) {
            if (!shouldRun()) {
                return false;
            }
        }

        sendVoteRequests(true);


        return false;
    }

    private void sendVoteRequests(boolean preVote) {

        List<RaftPeer> otherPeers = raftServer.otherPeers();
        if (otherPeers.isEmpty()) {
            // todo
            return;
        }
        long electionTerm;
        if (preVote) {
            electionTerm = raftServer.getState().getCurrentTerm().get();
        } else {
            // todo
            electionTerm = 0;
        }

        TermIndex lastEntry = raftServer.getRaftLog().getLastEntryTermIndex();
        RequestVoteRequest requestVoteRequest = new RequestVoteRequest();
        requestVoteRequest.setCandidateId(raftServer.getRaftPeer().getRaftPeerId());
        requestVoteRequest.setTerm(electionTerm);
        requestVoteRequest.setLastLogIndex(lastEntry.getIndex());
        requestVoteRequest.setLastLogTerm(lastEntry.getTerm());

        ExecutorCompletionService<RequestVoteResponse> voteExecutor =
                new ExecutorCompletionService<>(Executors.newFixedThreadPool(otherPeers.size()));
        for (RaftPeer raftPeer : otherPeers) {
            voteExecutor.submit(() -> raftServer.getRaftServerRpc().requestVote(requestVoteRequest));
        }

        int waitNum = otherPeers.size();
        while (waitNum > 0 && shouldRun()){

        }
    }

    private boolean shouldRun() {
        return running && raftServer.getState().getRole() == RaftRole.CANDIDATE;
    }
}
