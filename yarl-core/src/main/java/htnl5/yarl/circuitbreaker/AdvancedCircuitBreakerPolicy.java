package htnl5.yarl.circuitbreaker;

import java.time.Duration;

public final class AdvancedCircuitBreakerPolicy<R>
  extends CircuitBreakerPolicyBase<R, AdvancedCircuitBreakerPolicyBuilder<R>> implements ICircuitBreakerPolicy {
  AdvancedCircuitBreakerPolicy(final AdvancedCircuitBreakerPolicyBuilder<R> policyBuilder,
                               final ICircuitBreakerController<R> controller) {
    super(policyBuilder, controller);
  }

  public static <R> AdvancedCircuitBreakerPolicyBuilder<R> builder(final double failureThreshold,
                                                                   final Duration samplingDuration,
                                                                   final int minimumThroughput,
                                                                   final Duration durationOfBreak) {
    return new AdvancedCircuitBreakerPolicyBuilder<>(failureThreshold, samplingDuration, minimumThroughput,
      durationOfBreak);
  }
}
