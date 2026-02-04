package io.github.xinfra.lab.raft.protocol;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Data
@ToString
public class AppendEntriesResponse implements Serializable {

	boolean success;

	private Long term;

	private Long conflictIndex;

	private Long conflictTerm;

}
