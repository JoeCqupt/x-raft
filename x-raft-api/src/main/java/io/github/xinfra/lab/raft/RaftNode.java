package io.github.xinfra.lab.raft;

import io.github.xinfra.lab.raft.protocol.AdminProtocol;
import io.github.xinfra.lab.raft.protocol.RaftClientProtocol;
import io.github.xinfra.lab.raft.protocol.RaftServerProtocol;
import io.github.xinfra.lab.raft.log.RaftLog;

public interface RaftNode extends LifeCycle, RaftServerProtocol, RaftClientProtocol, AdminProtocol {

	RaftPeer raftPeer();

	String getRaftGroupId();

	RaftNodeOptions getRaftNodeOptions();

	RaftLog raftLog();

}
