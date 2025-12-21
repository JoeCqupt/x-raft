package io.github.xinfra.lab.raft;

import io.github.xinfra.lab.raft.protocol.AdminProtocol;
import io.github.xinfra.lab.raft.protocol.RaftClientProtocol;
import io.github.xinfra.lab.raft.protocol.RaftServerService;

public interface RaftNode extends LifeCycle, RaftServerService, RaftClientProtocol, AdminProtocol {

	String getRaftGroupId();

	String getRaftPeerId();

	String getRaftGroupPeerId();

	RaftPeer getRaftPeer();

	RaftRole getRaftRole();

}
