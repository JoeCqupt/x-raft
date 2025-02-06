package io.github.xinfra.lab.raft;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CandidateState extends Thread {

    private volatile boolean running = true;

    private final XRaftNode xRaftNode;

    public CandidateState(XRaftNode xRaftNode) {
        this.xRaftNode = xRaftNode;
    }

    @Override
    public void run() {
        while (shouldRun()) {
            try {
                if (preVote()) {
                    if (vote()) {
                        synchronized (xRaftNode) {
                            if (shouldRun()) {
                                xRaftNode.changeToLeader();
                                break;
                            }
                        }
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

    private boolean preVote() throws InterruptedException {
        long electionTerm;
        RaftConfiguration raftConfiguration;
        TermIndex lastEntryTermIndex;
        synchronized (xRaftNode) {
            if (!shouldRun()) {
                return false;
            }
            electionTerm = xRaftNode.getState().getCurrentTerm().get();
            raftConfiguration = xRaftNode.getState().getRaftConfiguration();
            lastEntryTermIndex = xRaftNode.getRaftLog().getLastEntryTermIndex();
        }

        VoteResult voteResult = doVote(true, electionTerm, raftConfiguration, lastEntryTermIndex);

        synchronized (xRaftNode) {
            if (!shouldRun()) {
                return false;
            }
        }

        switch (voteResult.getStatus()) {
            case PASSED:
                return true;
            case SHUTDOWN:
            case NOT_IN_CONF:
                // todo notify state machine
                xRaftNode.shutdown();
                return false;
            case TIMEOUT:
                return false; // retry
            case NEW_TERM:
                xRaftNode.changeToFollower(voteResult.getTerm());
                return false;
            case REJECTED:
                xRaftNode.changeToFollower();
                return false;
            default:
                throw new IllegalArgumentException("Unable to handle vote result " + voteResult);
        }
    }

    private VoteResult doVote(boolean preVote, long electionTerm, RaftConfiguration raftConfiguration,
                              TermIndex lastEntryTermIndex) throws InterruptedException {
        if (!(raftConfiguration.getConf().getVotingPeers().contains(xRaftNode.self()))) {
            return new VoteResult(electionTerm, Status.NOT_IN_CONF);
        }

        Set<RaftPeer> otherRaftPeers = raftConfiguration.getVotingRaftPeers();
        if (otherRaftPeers.isEmpty()) {
            return new VoteResult(electionTerm, Status.PASSED);
        }

        // init ballot box
        BallotBox ballotBox = new BallotBox(raftConfiguration);
        // vote to self
        ballotBox.grantVote(xRaftNode.self().getRaftPeerId());

        // todo: close it
        ExecutorCompletionService<VoteResponse> voteExecutor = new ExecutorCompletionService<>(
                Executors.newFixedThreadPool(otherRaftPeers.size()));
        for (RaftPeer raftPeer : otherRaftPeers) {
            // build request
            VoteRequest voteRequest = new VoteRequest();
            voteRequest.setPreVote(preVote);
            voteRequest.setCandidateId(xRaftNode.self().getRaftPeerId());
            voteRequest.setTerm(electionTerm);
            voteRequest.setLastLogIndex(lastEntryTermIndex.getIndex());
            voteRequest.setLastLogTerm(lastEntryTermIndex.getTerm());
            voteRequest.setRequestPeerId(xRaftNode.self().getRaftPeerId());
            voteRequest.setReplyPeerId(raftPeer.getRaftPeerId());

            voteExecutor.submit(() -> xRaftNode.getRaftServerTransport().requestVote(voteRequest));
        }

        int waitNum = otherRaftPeers.size();
        Long electionEndTimeMills = System.currentTimeMillis() + xRaftNode.getRandomElectionTimeoutMills();

        while (waitNum > 0 && shouldRun()) {
            long leftTimeMills = electionEndTimeMills - System.currentTimeMillis();
            if (leftTimeMills <= 0 && !ballotBox.isMajorityGranted()) {
                return new VoteResult(electionTerm, Status.TIMEOUT);
            } else if (leftTimeMills <= 0 && ballotBox.isMajorityGranted()) {
                return new VoteResult(electionTerm, Status.PASSED);
            }

            Future<VoteResponse> responseFuture = voteExecutor.poll(leftTimeMills, TimeUnit.MILLISECONDS);
            if (responseFuture == null) {
                // timeout
                continue;
            }

            VoteResponse voteResponse = null;
            try {
                voteResponse = responseFuture.get();

                if (voteResponse.isShouldShutdown()) {
                    return new VoteResult(electionTerm, Status.SHUTDOWN);
                }

                if (voteResponse.getTerm() > electionTerm) {
                    return new VoteResult(voteResponse.getTerm(), Status.NEW_TERM);
                }

                if (voteResponse.isVoteGranted()) {
                    ballotBox.grantVote(voteResponse.getReplyPeerId());
                    if (ballotBox.isMajorityGranted()) {
                        return new VoteResult(electionTerm, Status.PASSED);
                    }
                } else {
                    ballotBox.rejectVote(voteResponse.getReplyPeerId());
                    if (ballotBox.isMajorityRejected()) {
                        return new VoteResult(electionTerm, Status.REJECTED);
                    }
                }

            } catch (ExecutionException e) {
                log.error("get vote response error", e);
            }

            waitNum--;
        }

        // received all vote response
        if (ballotBox.isMajorityGranted()) {
            return new VoteResult(electionTerm, Status.PASSED);
        } else {
            return new VoteResult(electionTerm, Status.REJECTED);
        }
    }

    private boolean shouldRun() {
        return running && xRaftNode.getState().getRole() == RaftRole.CANDIDATE;
    }

    public void shutdown() {
        running = false;
    }

    enum Status {

        PASSED, REJECTED, TIMEOUT, NEW_TERM, SHUTDOWN, NOT_IN_CONF

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class VoteResult {

        private long term;

        private Status status;

    }

}
