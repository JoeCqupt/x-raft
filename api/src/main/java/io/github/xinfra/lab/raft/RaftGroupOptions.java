package io.github.xinfra.lab.raft;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.Validate;

import java.util.List;

public class RaftGroupOptions {


    @Setter
    @Getter
    String raftGroupId;

    @Getter
    List<RaftPeerId> peers;

    @Getter
    List<RaftPeerId> learners;


    @Getter
    @Setter
    private  RaftNodeOptions raftNodeOptions;


    public void setPeers(List<RaftPeerId> peers) {
        // check duplicate peerId
        Validate.isTrue(peers.stream().map(RaftPeerId::getPeerId).distinct().count() == peers.size(),
                "peers can not have duplicate peerId");
        // check duplicate address
        Validate.isTrue(peers.stream().map(RaftPeerId::getAddress).distinct().count() == peers.size(),
                "peers can not have duplicate address");
        this.peers = peers;
    }

    public void setLearners(List<RaftPeerId> learners) {
        // check duplicate peerId
        Validate.isTrue(learners.stream().map(RaftPeerId::getPeerId).distinct().count() == learners.size(),
                "peers can not have duplicate peerId");
        // check duplicate address
        Validate.isTrue(learners.stream().map(RaftPeerId::getAddress).distinct().count() == learners.size(),
                "peers can not have duplicate address");
        this.learners = learners;
    }
}
