package htnl5.yarl.circuitbreaker;

import java.time.Duration;
import java.util.Objects;

public final class CircuitBreakerPolicyBuilder<R>
  extends CircuitBreakerPolicyBuilderBase<R, CircuitBreakerPolicyBuilder<R>> {
  private int exceptionsAllowedBeforeBreaking = 1;

  private Duration durationOfBreak = Duration.ofMinutes(1);

  int getExceptionsAllowedBeforeBreaking() {
    return exceptionsAllowedBeforeBreaking;
  }

  public CircuitBreakerPolicyBuilder<R> exceptionsAllowedBeforeBreaking(final int exceptionsAllowedBeforeBreaking) {
    if (exceptionsAllowedBeforeBreaking <= 0)
      throw new IllegalArgumentException("exceptionsAllowedBeforeBreaking must be greater than zero.");
    this.exceptionsAllowedBeforeBreaking = exceptionsAllowedBeforeBreaking;
    return self();
  }

  Duration getDurationOfBreak() {
    return durationOfBreak;
  }

  public CircuitBreakerPolicyBuilder<R> durationOfBreak(final Duration durationOfBreak) {
    Objects.requireNonNull(durationOfBreak, "durationOfBreak must not be null.");
    if (durationOfBreak.isNegative())
      throw new IllegalArgumentException("durationOfBreak must be greater than or equal to zero.");
    this.durationOfBreak = durationOfBreak;
    return self();
  }

  @Override
  public CircuitBreakerPolicy<R, CircuitBreakerPolicyBuilder<R>> build() {
    final var controller = new ConsecutiveCountCircuitBreakerController<>(exceptionsAllowedBeforeBreaking,
      getDurationOfBreak(), getClock(), getOnBreak(), getOnReset(), getOnHalfOpen());
    return new CircuitBreakerPolicy<>(this, controller);
  }

  @Override
  protected CircuitBreakerPolicyBuilder<R> self() {
    return this;
  }
}
