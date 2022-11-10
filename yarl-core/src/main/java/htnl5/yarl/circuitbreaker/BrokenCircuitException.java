package htnl5.yarl.circuitbreaker;

import htnl5.yarl.ExecutionRejectedException;

public sealed class BrokenCircuitException extends ExecutionRejectedException
  permits BrokenCircuitWithResultException, IsolatedCircuitBreakerException {
  public BrokenCircuitException() {
  }

  public BrokenCircuitException(final String message) {
    super(message);
  }

  public BrokenCircuitException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public BrokenCircuitException(final Throwable cause) {
    super(cause);
  }

  public BrokenCircuitException(final String message, final Throwable cause, final boolean enableSuppression,
                                final boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
