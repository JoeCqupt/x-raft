package io.github.xinfra.lab.raft.protocol;

import lombok.Data;

import java.io.Serializable;

@Data
public class RequestMessage implements Serializable {

    /**
     * request target raft group id
     */
    String raftGroupId;

//    /**
//     * request target raft peer id
//     */
//    String raftPeerId;

}
