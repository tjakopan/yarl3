package htnl5.yarl.circuitbreaker;

import htnl5.yarl.IReactiveSyncPolicy;

import java.time.Duration;

public final class CircuitBreakerPolicy<R, B extends CircuitBreakerPolicyBuilderBase<R, B>>
  extends CircuitBreakerPolicyBase<R, B>
  implements IReactiveSyncPolicy<R> {
  CircuitBreakerPolicy(final B policyBuilder, final ICircuitBreakerController<R> controller) {
    super(policyBuilder, controller);
  }

  public static <R> CircuitBreakerPolicyBuilder<R> builder() {
    return new CircuitBreakerPolicyBuilder<>();
  }

  public static <R> AdvancedCircuitBreakerPolicyBuilder<R> advancedBuilder(final double failureThreshold,
                                                                           final Duration samplingDuration,
                                                                           final int minimumThroughput,
                                                                           final Duration durationOfBreak) {
    return new AdvancedCircuitBreakerPolicyBuilder<>(failureThreshold, samplingDuration, minimumThroughput,
      durationOfBreak);
  }
}
