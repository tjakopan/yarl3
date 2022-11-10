package htnl5.yarl.circuitbreaker;

public final class IsolatedCircuitBreakerException extends BrokenCircuitException {
  public IsolatedCircuitBreakerException(final String message) {
    super(message);
  }
}
