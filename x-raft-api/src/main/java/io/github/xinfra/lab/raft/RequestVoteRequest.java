package io.github.xinfra.lab.raft;

import lombok.Data;

import java.io.Serializable;

@Data
public class RequestVoteRequest implements Serializable {
    private Long term;
    private String candidateId;
    private Long lastLogIndex;
    private Long lastLogTerm;
}
