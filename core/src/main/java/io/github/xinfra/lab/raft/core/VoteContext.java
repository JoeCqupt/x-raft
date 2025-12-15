package io.github.xinfra.lab.raft.core;

import io.github.xinfra.lab.raft.RaftPeerId;
import io.github.xinfra.lab.raft.RaftRole;
import io.github.xinfra.lab.raft.log.TermIndex;
import io.github.xinfra.lab.raft.protocol.VoteRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public class VoteContext {

	private XRaftNode raftNode;

	private VoteRequest voteRequest;

	public VoteContext(XRaftNode raftNode, VoteRequest voteRequest) {
		this.raftNode = raftNode;
		this.voteRequest = voteRequest;
	}

	public boolean decideVote() {
		String candidateId = voteRequest.getCandidateId();
		// check candidate peer
        RaftPeerId candidate = raftNode.getConfigState().getCurrentConfiguration().getRaftPeerId(candidateId);
		if (candidate == null) {
			log.info("reject candidateId:{} not found.", candidateId);
			return false;
		}

		// check role
		RaftRole role = raftNode.getState().getRole();
		if (role == RaftRole.LEARNER) {
			log.info("reject candidateId:{} : current node role is learner", candidateId);
			return false;
		}

		// check term
		long currentTerm = raftNode.getState().getCurrentTerm().get();
		Long candidateTerm = voteRequest.getTerm();
		if (currentTerm > candidateTerm) {
			log.info("reject candidateId:{} : current term:{} > candidate term:{}", candidateId, currentTerm,
					candidateTerm);
			return false;
		}

		// check voteFor
		boolean preVote = voteRequest.isPreVote();
		if (!preVote && currentTerm == candidateTerm) {
			String voteFor = raftNode.getState().getVotedFor().get();
			if (voteFor != null && !Objects.equals(voteFor, candidateId)) {
				log.info("reject candidateId:{} : already votedFor:{}", candidateId, voteFor);
				return false;
			}
		}
		// check leadership
		if (preVote) {
			// todo check leadership
		}
		// todo

		// Check last log entry
		TermIndex candidateLastEntryTermIndex = new TermIndex(voteRequest.getLastLogTerm(),
				voteRequest.getLastLogIndex());
		TermIndex lastEntryTermIndex = raftNode.raftLog().getLastEntryTermIndex();

		final int compare = candidateLastEntryTermIndex.compareTo(lastEntryTermIndex);
		if (compare < 0) {
			log.info("reject candidateId:{} : candidate last entry:{} < current node:{}", candidateId,
					candidateLastEntryTermIndex, lastEntryTermIndex);
			return false;
		}

		// grant
		return true;
	}

}
