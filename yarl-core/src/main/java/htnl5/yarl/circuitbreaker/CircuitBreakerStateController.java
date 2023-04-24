package htnl5.yarl.circuitbreaker;

import htnl5.yarl.Context;
import htnl5.yarl.DelegateResult;
import htnl5.yarl.EventListener;

import java.time.Clock;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

abstract class CircuitBreakerStateController<R> implements ICircuitBreakerController<R> {
  protected final Duration durationOfBreak;
  protected final Clock clock;
  protected final EventListener<BreakEvent<? extends R>> onBreak;
  protected final Consumer<Context> onReset;
  protected final Runnable onHalfOpen;

  protected CircuitBreakerState state;
  protected long blockedUntil;
  protected DelegateResult<R> lastOutcome;

  protected final Lock lock = new ReentrantLock();

  protected CircuitBreakerStateController(final Duration durationOfBreak, final Clock clock,
                                          final EventListener<BreakEvent<? extends R>> onBreak,
                                          final Consumer<Context> onReset, final Runnable onHalfOpen) {
    this.durationOfBreak = durationOfBreak;
    this.clock = clock;
    this.onBreak = onBreak;
    this.onReset = onReset;
    this.onHalfOpen = onHalfOpen;

    state = CircuitBreakerState.CLOSED;
    blockedUntil = 0L;
    lastOutcome = null;
  }

  @Override
  public CircuitBreakerState getState() {
    if (state != CircuitBreakerState.OPEN) return state;
    lock.lock();
    try {
      if (state == CircuitBreakerState.OPEN && !isInAutomatedBreak()) {
        state = CircuitBreakerState.HALF_OPEN;
        onHalfOpen.run();
      }
      return state;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public Optional<DelegateResult<R>> getLastOutcome() {
    lock.lock();
    try {
      return Optional.ofNullable(lastOutcome);
    } finally {
      lock.unlock();
    }
  }

  private boolean isInAutomatedBreak() {
    lock.lock();
    try {
      return clock.millis() < blockedUntil;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void isolate() {
    lock.lock();
    try {
      lastOutcome = DelegateResult.failure(new IsolatedCircuitBreakerException("The circuit is manually held open and" +
        " is not allowing calls."));
      breakFor(Duration.ofMillis(Long.MAX_VALUE), Context.none());
      state = CircuitBreakerState.ISOLATED;
    } finally {
      lock.unlock();
    }
  }

  protected void break_(final Context context) {
    breakFor(durationOfBreak, context);
  }

  private void breakFor(final Duration durationOfBreak, final Context context) {
    lock.lock();
    try {
      final var willDurationTakeUsPastDateMaxValue = durationOfBreak.toMillis() > Long.MAX_VALUE - clock.millis();
      blockedUntil = willDurationTakeUsPastDateMaxValue
        ? new Date(Long.MAX_VALUE).getTime()
        : clock.millis() + durationOfBreak.toMillis();
      final var transitionedState = state;
      state = CircuitBreakerState.OPEN;
      onBreak.accept(new BreakEvent<>(lastOutcome, transitionedState, durationOfBreak, context));
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void reset() {
    reset(new Context());
  }

  protected abstract void resetSpecific();

  protected void reset(final Context context) {
    resetSpecific();

    lock.lock();
    try {
      blockedUntil = 0L;
      lastOutcome = null;
      final var priorState = state;
      state = CircuitBreakerState.CLOSED;
      if (priorState != CircuitBreakerState.CLOSED) {
        onReset.accept(context);
      }
    } finally {
      lock.unlock();
    }
  }

  protected boolean permitHalfOpen() {
    final var currentlyBlockedUntil = blockedUntil;
    if (clock.millis() >= currentlyBlockedUntil) {
      lock.lock();
      try {
        final var originalBlockedUntil = blockedUntil;
        if (blockedUntil == currentlyBlockedUntil) {
          blockedUntil = clock.millis() + durationOfBreak.toMillis();
        }
        return originalBlockedUntil == currentlyBlockedUntil;
      } finally {
        lock.unlock();
      }
    }
    return false;
  }

  private BrokenCircuitException getBreakingException() {
    final var brokenCircuitMessage = "The circuit is now open and is not allowing calls.";
    final var lastOutcome = this.lastOutcome;
    if (lastOutcome == null) return new BrokenCircuitException(brokenCircuitMessage);
    return lastOutcome.match(r -> new BrokenCircuitWithResultException(brokenCircuitMessage, r),
      e -> new BrokenCircuitException(brokenCircuitMessage, e));
  }

  @Override
  public void onActionPreExecute() throws BrokenCircuitException {
    switch (getState()) {
      case CLOSED -> {
      }
      case OPEN -> throw getBreakingException();
      case HALF_OPEN -> {
        if (!permitHalfOpen()) throw getBreakingException();
      }
      case ISOLATED ->
        throw new IsolatedCircuitBreakerException("The circuit is manually held open and is not allowing calls.");
    }
  }
}
