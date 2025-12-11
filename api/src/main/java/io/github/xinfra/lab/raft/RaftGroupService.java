package io.github.xinfra.lab.raft;

public interface RaftGroupService {

    RaftNode getRaftNode();

    String getRaftGroupId();
}
