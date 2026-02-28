package io.github.xinfra.lab.raft;

import io.github.xinfra.lab.raft.protocol.RaftAdminProtocol;
import io.github.xinfra.lab.raft.protocol.RaftClientProtocol;
import io.github.xinfra.lab.raft.protocol.RaftServerService;

public interface RaftNode extends LifeCycle, RaftServerService, RaftClientProtocol, RaftAdminProtocol {

	String getRaftGroupId();

	String getRaftPeerId();

	String getRaftGroupPeerId();

	RaftPeer getRaftPeer();

	RaftRole getRaftRole();

	void notifyLogAppended();

}
