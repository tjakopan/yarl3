package htnl5.yarl.circuitbreaker;

public final class BrokenCircuitWithResultException extends BrokenCircuitException {
  private final Object result;

  public BrokenCircuitWithResultException(final Object result) {
    this.result = result;
  }

  public BrokenCircuitWithResultException(final String message, final Object result) {
    super(message);
    this.result = result;
  }

  public BrokenCircuitWithResultException(final String message, final boolean enableSuppression,
                                          final boolean writableStackTrace, final Object result) {
    super(message, null, enableSuppression, writableStackTrace);
    this.result = result;
  }

  public Object getResult() {
    return result;
  }
}
