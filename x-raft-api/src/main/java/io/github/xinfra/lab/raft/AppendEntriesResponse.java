package io.github.xinfra.lab.raft;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class AppendEntriesResponse extends TransportInfo implements Serializable {

	private Long term;

	private Boolean success;

}
