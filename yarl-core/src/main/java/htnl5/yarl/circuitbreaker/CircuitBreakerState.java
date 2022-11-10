package htnl5.yarl.circuitbreaker;

public enum CircuitBreakerState {
  CLOSED,
  OPEN,
  HALF_OPEN,
  ISOLATED
}
