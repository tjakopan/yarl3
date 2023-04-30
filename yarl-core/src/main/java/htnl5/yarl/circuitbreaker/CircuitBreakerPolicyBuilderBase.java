package htnl5.yarl.circuitbreaker;

import htnl5.yarl.*;

import java.time.Clock;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class CircuitBreakerPolicyBuilderBase<R, B extends CircuitBreakerPolicyBuilderBase<R, B>>
  extends PolicyBuilder<B>
  implements IReactivePolicyBuilder<R, B> {
  private final ResultPredicates<R> resultPredicates = ResultPredicates.none();
  private final ExceptionPredicates exceptionPredicates = ExceptionPredicates.none();
  private Clock clock = Clock.systemUTC();
  private EventListener<BreakEvent<? extends R>> onBreak = event -> {
  };
  private Consumer<Context> onReset = ctx -> {
  };
  private Runnable onHalfOpen = () -> {
  };

  @Override
  public ResultPredicates<R> getResultPredicates() {
    return resultPredicates;
  }

  @Override
  public ExceptionPredicates getExceptionPredicates() {
    return exceptionPredicates;
  }

  Clock getClock() {
    return clock;
  }

  B clock(final Clock clock) {
    Objects.requireNonNull(clock, "clock must not be null.");
    this.clock = clock;
    return self();
  }

  EventListener<BreakEvent<? extends R>> getOnBreak() {
    return onBreak;
  }

  public B onBreak(final EventListener<BreakEvent<? extends R>> onBreak) {
    Objects.requireNonNull(onBreak, "onBreak must not be null.");
    this.onBreak = onBreak;
    return self();
  }

  Consumer<Context> getOnReset() {
    return onReset;
  }

  public B onReset(final Consumer<Context> onReset) {
    Objects.requireNonNull(onReset, "onReset must not be null.");
    this.onReset = onReset;
    return self();
  }

  Runnable getOnHalfOpen() {
    return onHalfOpen;
  }

  public B onHalfOpen(final Runnable onHalfOpen) {
    Objects.requireNonNull(onHalfOpen, "onHalfOpen must not be null.");
    this.onHalfOpen = onHalfOpen;
    return self();
  }
}
