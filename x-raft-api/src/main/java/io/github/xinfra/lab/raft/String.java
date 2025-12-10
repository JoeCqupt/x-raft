package io.github.xinfra.lab.raft;

import lombok.Data;
import org.apache.commons.lang3.Validate;

import java.util.List;

@Data
public class String {

    private java.lang.String raftGroupId;

    private List<RaftPeer> peers;

    public String(java.lang.String raftGroupId, List<RaftPeer> peers) {
        Validate.notBlank(raftGroupId, "raftGroupId can not be blank");
        Validate.notEmpty(peers, "peers can not be empty");
        // 判断peerId 是否有重复
        Validate.isTrue(peers.stream().map(RaftPeer::getRaftPeerId).distinct().count() == peers.size(), "peers can not have duplicate peerId");
        // 判断地址 是否有重复
        Validate.isTrue(peers.stream().map(RaftPeer::getAddress).distinct().count() == peers.size(), "peers can not have duplicate address");

        this.raftGroupId = raftGroupId;
        this.peers = peers;
    }

}
