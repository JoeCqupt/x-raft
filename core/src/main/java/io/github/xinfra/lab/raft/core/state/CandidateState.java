package io.github.xinfra.lab.raft.core.state;

import io.github.xinfra.lab.raft.RaftPeerId;
import io.github.xinfra.lab.raft.RaftRole;
import io.github.xinfra.lab.raft.conf.ConfigurationEntry;
import io.github.xinfra.lab.raft.core.XRaftNode;
import io.github.xinfra.lab.raft.core.transport.RaftApi;
import io.github.xinfra.lab.raft.log.TermIndex;
import io.github.xinfra.lab.raft.protocol.VoteRequest;
import io.github.xinfra.lab.raft.protocol.VoteResponse;
import io.github.xinfra.lab.raft.transport.CallOptions;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CandidateState extends Thread {

    private volatile boolean running;

    private final XRaftNode xRaftNode;

    private Thread electionTask;

    public CandidateState(XRaftNode xRaftNode) {
        this.xRaftNode = xRaftNode;
    }

    public void startup() {
        if (running) {
            return;
        }
        running = true;
        electionTask = new ElectionTask();
        electionTask.start();
    }

    public void shutdown() {
        if (!running) {
            return;
        }
        running = false;
        electionTask.interrupt();
        electionTask = null;
    }

    class ElectionTask extends Thread {

        public ElectionTask() {
            super("ElectionTask");
        }

        @Override
        public void run() {
            while (shouldRun()) {
                try {
                    if (askForVotes(true)) {
                        if (askForVotes(false)) {
                            if (shouldRun() && xRaftNode.getState().changeToLeader()) {
                                break;
                            }
                        }
                    }
                    Thread.sleep(xRaftNode.getRandomElectionTimeoutMills());
                } catch (InterruptedException e) {
                    log.info("ElectionTask thread interrupted");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Throwable t) {
                    log.error("ElectionTask thread run ex", t);
                }
            }
        }

        private boolean askForVotes(boolean preVote) throws InterruptedException {
            Long electionTerm;
            ConfigurationEntry configurationEntry;
            TermIndex lastEntryTermIndex;

            if (!shouldRun()) {
                return false;
            }

            // todo: notify state machine
            xRaftNode.getState().getLeaderId().getAndSet(null);
            if (preVote) {
                electionTerm = xRaftNode.getState().getCurrentTerm().get();
            } else {
                electionTerm = xRaftNode.getState().getCurrentTerm().incrementAndGet();
                xRaftNode.getState().getVotedFor().getAndSet(xRaftNode.raftPeerId().getPeerId());
                xRaftNode.getState().persistMetadata();
            }
            configurationEntry = xRaftNode.getConfigState().getCurrentConfiguration();
            lastEntryTermIndex = xRaftNode.raftLog().getLastEntryTermIndex();


            VoteResult voteResult = askForVotes(preVote, electionTerm, configurationEntry, lastEntryTermIndex);

            if (!shouldRun()) {
                return false;
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
                case FAIL:
                    return false; // retry
                case NEW_TERM:
                    xRaftNode.getState().changeToFollower(voteResult.getTerm());
                    return false;
                case REJECTED:
                    xRaftNode.getState().changeToFollower();
                    return false;
                default:
                    throw new IllegalArgumentException("Unable to handle vote result " + voteResult);
            }
        }

        private VoteResult askForVotes(boolean preVote, Long electionTerm, ConfigurationEntry configurationEntry,
                                       TermIndex lastEntryTermIndex) throws InterruptedException {
            if (!(configurationEntry.getPeers().contains(xRaftNode.raftPeerId()))) {
                return new VoteResult(electionTerm, Status.NOT_IN_CONF);
            }

            List<RaftPeerId> otherVotingRaftPeerIds = configurationEntry.getPeers();
            // remove self
            otherVotingRaftPeerIds.remove(xRaftNode.raftPeerId());

            if (otherVotingRaftPeerIds.isEmpty()) {
                return new VoteResult(electionTerm, Status.PASSED);
            }

            // init ballot box
            BallotBox ballotBox = new BallotBox(configurationEntry);
            // vote to raftPeerId
            ballotBox.grantVote(xRaftNode.raftPeerId().getPeerId());

            // todo: close it
            ExecutorCompletionService<VoteResponse> voteExecutor = new ExecutorCompletionService<>(
                    Executors.newFixedThreadPool(otherVotingRaftPeerIds.size()));
            for (RaftPeerId raftPeerId : otherVotingRaftPeerIds) {
                // build request
                VoteRequest voteRequest = new VoteRequest();
                voteRequest.setRaftGroupId(xRaftNode.raftGroupId());
                voteRequest.setPreVote(preVote);
                voteRequest.setCandidateId(xRaftNode.raftPeerId().getPeerId());
                voteRequest.setTerm(electionTerm);
                voteRequest.setLastLogIndex(lastEntryTermIndex.getIndex());
                voteRequest.setLastLogTerm(lastEntryTermIndex.getTerm());
                voteRequest.setRequestPeerId(xRaftNode.raftPeerId().getPeerId());
                voteRequest.setReplyPeerId(raftPeerId.getPeerId());

                // todo: add timeout & async call
                voteExecutor.submit(() -> xRaftNode.getTransportClient()
                        .blockingCall(RaftApi.requestVote, raftPeerId.getAddress(), voteRequest, new CallOptions()));
            }

            int waitNum = otherVotingRaftPeerIds.size();
            Long electionEndTimeMills = System.currentTimeMillis() + xRaftNode.getRandomElectionTimeoutMills();

            while (waitNum > 0 && shouldRun()) {
                Long leftTimeMills = electionEndTimeMills - System.currentTimeMillis();
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
                    if (!voteResponse.isSuccess()) {
                        //todo： 请求失败 不能直接失败
                        return new VoteResult(electionTerm, Status.FAIL);
                    }

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

                } catch (Exception e) {
                    log.error("get vote response error", e);
                    // todo： 请求失败 不能直接失败
                    return new VoteResult(electionTerm, Status.FAIL);
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
            return running && xRaftNode.getState().getRole() == RaftRole.CANDIDATE
                    && !Thread.currentThread().isInterrupted();
        }

    }

    enum Status {

        PASSED, REJECTED, FAIL, TIMEOUT, NEW_TERM, SHUTDOWN, NOT_IN_CONF

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class VoteResult {

        private Long term;

        private Status status;

    }

}
