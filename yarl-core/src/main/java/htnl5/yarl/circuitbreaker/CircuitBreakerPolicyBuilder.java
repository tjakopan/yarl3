package htnl5.yarl.circuitbreaker;

import htnl5.yarl.Context;
import htnl5.yarl.EventListener;
import htnl5.yarl.PolicyBuilder;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;

public final class CircuitBreakerPolicyBuilder<R> extends PolicyBuilder<R, CircuitBreakerPolicyBuilder<R>> {
  private int exceptionsAllowedBeforeBreaking = 1;
  private Duration durationOfBreak = Duration.ofMinutes(1);

  private Clock clock = Clock.systemUTC();

  private EventListener<BreakEvent<? extends R>> onBreak = event -> {
  };

  private Consumer<Context> onReset = ctx -> {
  };

  private Runnable onHalfOpen = () -> {
  };

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

  Clock getClock() {
    return clock;
  }

  CircuitBreakerPolicyBuilder<R> clock(final Clock clock) {
    Objects.requireNonNull(clock, "clock must not be null.");
    this.clock = clock;
    return self();
  }

  EventListener<BreakEvent<? extends R>> getOnBreak() {
    return onBreak;
  }

  public CircuitBreakerPolicyBuilder<R> onBreak(final EventListener<BreakEvent<? extends R>> onBreak) {
    Objects.requireNonNull(onBreak, "onBreak must not be null.");
    this.onBreak = onBreak;
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
