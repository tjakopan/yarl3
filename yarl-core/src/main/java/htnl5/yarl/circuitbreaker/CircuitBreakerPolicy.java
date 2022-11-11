package htnl5.yarl.circuitbreaker;

public final class CircuitBreakerPolicy<R> extends CircuitBreakerPolicyBase<R, CircuitBreakerPolicyBuilder<R>>
  implements ICircuitBreakerPolicy {
  CircuitBreakerPolicy(final CircuitBreakerPolicyBuilder<R> policyBuilder,
                       final ICircuitBreakerController<R> controller) {
    super(policyBuilder, controller);
  }

  public static <R> CircuitBreakerPolicyBuilder<R> builder() {
    return new CircuitBreakerPolicyBuilder<>();
  }
}
