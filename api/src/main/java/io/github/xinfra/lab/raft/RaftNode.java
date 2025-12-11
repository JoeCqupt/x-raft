package io.github.xinfra.lab.raft;

import io.github.xinfra.lab.raft.protocol.AdminProtocol;
import io.github.xinfra.lab.raft.protocol.RaftClientProtocol;
import io.github.xinfra.lab.raft.protocol.RaftServerProtocol;
import io.github.xinfra.lab.raft.log.RaftLog;

import java.lang.String;

public interface RaftNode extends LifeCycle, RaftServerProtocol, RaftClientProtocol, AdminProtocol {

	RaftPeerId raftPeer();

	String getRaftGroupId();

	RaftNodeOptions getRaftNodeOptions();

	RaftLog raftLog();

}
