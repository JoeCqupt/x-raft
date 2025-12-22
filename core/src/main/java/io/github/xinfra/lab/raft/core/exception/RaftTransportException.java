package io.github.xinfra.lab.raft.core.exception;

public class RaftTransportException extends RaftException {
  public RaftTransportException() {
  }

  public RaftTransportException(String message) {
    super(message);
  }

  public RaftTransportException(String message, Throwable cause) {
    super(message, cause);
  }

  public RaftTransportException(Throwable cause) {
    super(cause);
  }

  public RaftTransportException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
