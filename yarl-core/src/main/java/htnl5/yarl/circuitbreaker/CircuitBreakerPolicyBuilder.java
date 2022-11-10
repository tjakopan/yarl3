package htnl5.yarl.circuitbreaker;

import htnl5.yarl.Context;
import htnl5.yarl.PolicyBuilder;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;

public final class CircuitBreakerPolicyBuilder<R> extends PolicyBuilder<R, CircuitBreakerPolicyBuilder<R>> {
  private final int exceptionsAllowedBeforeBreaking;
  private final Duration durationOfBreak;

  private Clock clock = Clock.systemUTC();

  private OnBreakListener<R> onBreak = event -> {
  };

  private Consumer<Context> onReset = ctx -> {
  };

  private Runnable onHalfOpen = () -> {
  };

  public CircuitBreakerPolicyBuilder(final int exceptionsAllowedBeforeBreaking, final Duration durationOfBreak) {
    if (exceptionsAllowedBeforeBreaking <= 0)
      throw new IllegalArgumentException("exceptionsAllowedBeforeBreaking must be greater than zero.");
    Objects.requireNonNull(durationOfBreak, "durationOfBreak must not be null.");
    if (durationOfBreak.isNegative())
      throw new IllegalArgumentException("durationOfBreak must be greater than or equal to zero.");
    this.exceptionsAllowedBeforeBreaking = exceptionsAllowedBeforeBreaking;
    this.durationOfBreak = durationOfBreak;
  }

  int getExceptionsAllowedBeforeBreaking() {
    return exceptionsAllowedBeforeBreaking;
  }

  Duration getDurationOfBreak() {
    return durationOfBreak;
  }

  Clock getClock() {
    return clock;
  }

  CircuitBreakerPolicyBuilder<R> clock(final Clock clock) {
    Objects.requireNonNull(clock, "clock must not be null.");
    this.clock = clock;
    return self();
  }

  OnBreakListener<R> getOnBreak() {
    return onBreak;
  }

  public CircuitBreakerPolicyBuilder<R> onBreak(final OnBreakListener<? super R> onBreak) {
    Objects.requireNonNull(onBreak, "onBreak must not be null.");
    this.onBreak = onBreak::accept;
    return self();
  }

  Consumer<Context> getOnReset() {
    return onReset;
  }

  public CircuitBreakerPolicyBuilder<R> onReset(final Consumer<Context> onReset) {
    Objects.requireNonNull(onReset, "onReset must not be null.");
    this.onReset = onReset;
    return self();
  }

  Runnable getOnHalfOpen() {
    return onHalfOpen;
  }

  public CircuitBreakerPolicyBuilder<R> onHalfOpen(final Runnable onHalfOpen) {
    Objects.requireNonNull(onHalfOpen, "onHalfOpen must not be null.");
    this.onHalfOpen = onHalfOpen;
    return self();
  }

  @Override
  public CircuitBreakerPolicy<R> build() {
    final var controller = new ConsecutiveCountCircuitBreakerController<>(exceptionsAllowedBeforeBreaking,
      durationOfBreak, clock, onBreak, onReset, onHalfOpen);
    return new CircuitBreakerPolicy<>(this, controller);
  }

  @Override
  protected CircuitBreakerPolicyBuilder<R> self() {
    return this;
  }
}
