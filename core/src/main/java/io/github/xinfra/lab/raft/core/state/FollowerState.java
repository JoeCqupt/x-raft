package io.github.xinfra.lab.raft.core.state;

import io.github.xinfra.lab.raft.RaftPeer;
import io.github.xinfra.lab.raft.RaftRole;
import io.github.xinfra.lab.raft.conf.ConfigurationEntry;
import io.github.xinfra.lab.raft.core.XRaftNode;
import io.github.xinfra.lab.raft.core.transport.RaftApi;
import io.github.xinfra.lab.raft.log.TermIndex;
import io.github.xinfra.lab.raft.protocol.VoteRequest;
import io.github.xinfra.lab.raft.protocol.VoteResponse;
import io.github.xinfra.lab.raft.transport.CallOptions;
import io.github.xinfra.lab.raft.transport.ResponseCallBack;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class FollowerState {

    private final XRaftNode xRaftNode;

    private volatile boolean running;

    private Thread electionTimeoutTask;

    private BallotBox preVoteBallotBox;

    public FollowerState(XRaftNode xRaftNode) {
        this.xRaftNode = xRaftNode;
    }


    public void startup() {
        if (running) {
            return;
        }
        running = true;
        electionTimeoutTask = new ElectionTimeoutTask();
        electionTimeoutTask.start();
    }

    public void shutdown() {
        if (!running) {
            return;
        }
        running = false;
        electionTimeoutTask.interrupt();
        electionTimeoutTask = null; // help gc
    }

    class ElectionTimeoutTask extends Thread {

        public ElectionTimeoutTask() {
            super(String.format("ElectionTimeoutThread-%s", xRaftNode.getRaftGroupPeerId()));
        }

        @Override
        public void run() {
            while (shouldRun()) {
                try {
                    Long electionTimeoutMills = xRaftNode.getRaftNodeOptions().getRandomElectionTimeoutMills();
                    TimeUnit.MILLISECONDS.sleep(electionTimeoutMills);
                    try {
                        xRaftNode.getState().getWriteLock().lock();
                        if (xRaftNode.getState().getRole() != RaftRole.FOLLOWER) {
                            log.info("ElectionTimeoutThread exit, current role is {}", xRaftNode.getState().getRole());
                            break;
                        }
                        if (timeout(electionTimeoutMills)) {
                            preVote();
                        }
                    } finally {
                        xRaftNode.getState().getWriteLock().unlock();
                    }
                } catch (InterruptedException e) {
                    log.info("ElectionTimeoutThread interrupted");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Throwable t) {
                    log.error("ElectionTimeoutThread ex", t);
                }
            }
        }

        public boolean timeout(long electionTimeoutMills) {
            return System.currentTimeMillis() - xRaftNode.getState().getLastLeaderRpcTimeMills() >= electionTimeoutMills;
        }

        private boolean shouldRun() {
            return running && !Thread.currentThread().isInterrupted();
        }

    }


    private void preVote() throws Exception {
        log.info("node:{} start preVote", xRaftNode.getRaftGroupPeerId());
        // reset leader
        xRaftNode.getState().resetLeaderId(null);

        long term = xRaftNode.getState().getCurrentTerm();
        TermIndex lastLogIndex = xRaftNode.getState().getRaftLog().getLastEntryTermIndex();
        ConfigurationEntry config = xRaftNode.getState().getConfigState().getCurrentConfig();
        preVoteBallotBox = new BallotBox(config);

        CallOptions callOptions = new CallOptions();
        callOptions.setTimeoutMs(xRaftNode.getRaftNodeOptions().getElectionTimeoutMills());
        PreVoteResponseCallBack callBack = new PreVoteResponseCallBack(preVoteBallotBox);
        for (RaftPeer raftPeer : config.getPeers()) {
            if (xRaftNode.getRaftPeer().equals(raftPeer)) {
                continue;
            }
            VoteRequest voteRequest = new VoteRequest();
            voteRequest.setRaftGroupId(xRaftNode.getRaftGroupPeerId());
            voteRequest.setPreVote(true);
            voteRequest.setTerm(term + 1);
            voteRequest.setCandidateId(xRaftNode.getRaftPeer().getRaftPeerId());
            voteRequest.setLastLogIndex(lastLogIndex.getIndex());
            voteRequest.setLastLogTerm(lastLogIndex.getTerm());

            xRaftNode.getTransportClient().asyncCall(RaftApi.requestVote,
                    voteRequest,
                    raftPeer.getAddress(),
                    callOptions,
                    callBack
            );
        }
        // grant self vote
        preVoteBallotBox.grantVote(xRaftNode.getRaftPeer().getRaftPeerId());
        if (preVoteBallotBox.isMajorityGranted()) {
            xRaftNode.getState().changeToCandidate();
        }
    }

    class PreVoteResponseCallBack implements ResponseCallBack<VoteResponse> {
        private final BallotBox ballotBox;

        public PreVoteResponseCallBack(BallotBox ballotBox) {
            this.ballotBox = ballotBox;
        }

        @Override
        public void onResponse(VoteResponse response) {
            try {
                xRaftNode.getState().getWriteLock().lock();
                if (ballotBox != preVoteBallotBox) {
                    log.warn("PreVoteResponseCallBack is outdated");
                }
                // todo

            } finally {
                xRaftNode.getState().getWriteLock().unlock();
            }
        }

        @Override
        public void onError(Throwable throwable) {
            // todo
        }
    }

}
