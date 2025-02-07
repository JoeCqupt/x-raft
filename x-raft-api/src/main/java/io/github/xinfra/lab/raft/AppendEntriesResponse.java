package io.github.xinfra.lab.raft;

import lombok.Data;

import java.io.Serializable;

@Data
public class AppendEntriesResponse extends TransportInfo implements Serializable {

	private Long term;

	private Boolean success;

}
