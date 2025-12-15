package io.github.xinfra.lab.raft;

import io.github.xinfra.lab.raft.protocol.AdminProtocol;
import io.github.xinfra.lab.raft.protocol.RaftClientProtocol;
import io.github.xinfra.lab.raft.protocol.RaftProtocol;
import io.github.xinfra.lab.raft.log.RaftLog;

public interface RaftNode extends LifeCycle, RaftProtocol, RaftClientProtocol, AdminProtocol {

	String raftGroupId();

	RaftPeerId raftPeerId();

	RaftLog raftLog();

	RaftRole raftRole();

}
