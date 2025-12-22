package io.github.xinfra.lab.raft.protocol;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class AppendEntriesResponse extends ResponseMessage implements Serializable {

	private Long term;

}
