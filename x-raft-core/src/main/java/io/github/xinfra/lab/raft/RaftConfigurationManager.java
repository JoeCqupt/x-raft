package io.github.xinfra.lab.raft;

import lombok.Getter;

public class RaftConfigurationManager {

    @Getter
    private RaftConfiguration initialConfiguration;

    public RaftConfigurationManager(RaftConfiguration initialConfiguration) {
        this.initialConfiguration = initialConfiguration;
    }


}
