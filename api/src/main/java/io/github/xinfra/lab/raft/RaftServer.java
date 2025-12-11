package io.github.xinfra.lab.raft;

import java.util.List;

public interface RaftServer extends LifeCycle {

    List<String> getRaftGroupIds();

}
