package htnl5.yarl.circuitbreaker;

import htnl5.yarl.IBuildable;

import java.time.Duration;

public final class AdvancedCircuitBreakerPolicyBuilder<R>
  extends CircuitBreakerPolicyBuilderBase<R, AdvancedCircuitBreakerPolicyBuilder<R>>
  implements IBuildable<CircuitBreakerPolicy<R, AdvancedCircuitBreakerPolicyBuilder<R>>> {
  private final double failureThreshold;

  private final Duration samplingDuration;

  private final int minimumThroughput;

  private final Duration durationOfBreak;

  public AdvancedCircuitBreakerPolicyBuilder(final double failureThreshold, final Duration samplingDuration,
                                             final int minimumThroughput, final Duration durationOfBreak) {
    final var resolutionOfCircuit =
      Duration.ofMillis(AdvancedCircuitBreakerController.RESOLUTION_OF_CIRCUIT_TIMER_MILLIS);
    if (failureThreshold <= 0d) throw new IllegalArgumentException("failureThreshold must be greater than zero.");
    if (failureThreshold > 1d)
      throw new IllegalArgumentException("failureThreshold must me less than or equal to one.");
    if (samplingDuration.compareTo(resolutionOfCircuit) < 0)
      throw new IllegalArgumentException(("samplingDuration must be equal to or greater than %d milliseconds. This is" +
        " " +
        "the minimum resolution of the circuit breaker timer.").formatted(AdvancedCircuitBreakerController.RESOLUTION_OF_CIRCUIT_TIMER_MILLIS));
    if (minimumThroughput <= 1) throw new IllegalArgumentException("minimumThroughput must be greater than one.");
    if (durationOfBreak.isNegative())
      throw new IllegalArgumentException("durationOfBreak must be greater than or equal to zero.");
    this.failureThreshold = failureThreshold;
    this.samplingDuration = samplingDuration;
    this.minimumThroughput = minimumThroughput;
    this.durationOfBreak = durationOfBreak;
  }

  double getFailureThreshold() {
    return failureThreshold;
  }

  Duration getSamplingDuration() {
    return samplingDuration;
  }

  int getMinimumThroughput() {
    return minimumThroughput;
  }

  Duration getDurationOfBreak() {
    return durationOfBreak;
  }

  @Override
  public CircuitBreakerPolicy<R, AdvancedCircuitBreakerPolicyBuilder<R>> build() {
    final var controller = new AdvancedCircuitBreakerController<>(failureThreshold, samplingDuration,
      minimumThroughput, durationOfBreak, getClock(), getOnBreak(), getOnReset(), getOnHalfOpen());
    return new CircuitBreakerPolicy<>(this, controller);
  }

  @Override
  public AdvancedCircuitBreakerPolicyBuilder<R> self() {
    return this;
  }
}
