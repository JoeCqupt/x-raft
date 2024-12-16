package io.github.xinfra.lab.raft;

import lombok.extern.slf4j.Slf4j;

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
            try{
                
               if (preVote()) {
                   if (vote()){
                       raftServer.changeToLeader();
                   }
               }

            } catch (Throwable t){
                log.error("CandidateState error", t);
            }
        }
    }

    private boolean vote() {
    }

    private boolean preVote() {
    }

    private boolean shouldRun() {
        return running && raftServer.getState().getRole() == RaftRole.CANDIDATE;
    }
}
