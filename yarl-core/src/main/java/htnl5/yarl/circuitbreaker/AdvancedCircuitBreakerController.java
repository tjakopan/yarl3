package htnl5.yarl.circuitbreaker;

import htnl5.yarl.Context;
import htnl5.yarl.DelegateResult;
import htnl5.yarl.EventListener;

import java.time.Clock;
import java.time.Duration;
import java.util.function.Consumer;

class AdvancedCircuitBreakerController<R> extends CircuitBreakerStateController<R> {
  private static final int NUMBER_OF_WINDOWS = 10;
  static final long RESOLUTION_OF_CIRCUIT_TIMER_MILLIS = 20;

  private final IHealthMetrics metrics;
  private final double failureThreshold;
  private final int minimumThroughput;

  AdvancedCircuitBreakerController(final double failureThreshold, final Duration samplingDuration,
                                   final int minimumThroughput, final Duration durationOfBreak, final Clock clock,
                                   final EventListener<BreakEvent<? extends R>> onBreak,
                                   final Consumer<Context> onReset, final Runnable onHalfOpen) {
    super(durationOfBreak, clock, onBreak, onReset, onHalfOpen);
    metrics = samplingDuration.toMillis() < RESOLUTION_OF_CIRCUIT_TIMER_MILLIS * NUMBER_OF_WINDOWS
      ? new SingleHealthMetrics(samplingDuration, clock)
      : new RollingHealthMetrics(samplingDuration, clock, NUMBER_OF_WINDOWS);
    this.failureThreshold = failureThreshold;
    this.minimumThroughput = minimumThroughput;
  }

  @Override
  protected void resetSpecific() {
    lock.lock();
    try {
      metrics.reset();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void onActionSuccess(final Context context) {
    lock.lock();
    try {
      switch (state) {
        case CLOSED, OPEN, ISOLATED -> {
        }
        case HALF_OPEN -> reset(context);
      }
      metrics.incrementSuccess();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void onActionFailure(final DelegateResult<R> outcome, final Context context) {
    lock.lock();
    try {
      lastOutcome = outcome;
      switch (state) {
        case CLOSED -> {
          metrics.incrementFailure();
          final var healthCount = metrics.getHealthCount();
          final var throughput = healthCount.getTotal();
          if (throughput >= minimumThroughput && ((double) healthCount.getFailures()) / throughput >= failureThreshold) {
            break_(context);
          }
        }
        case OPEN, ISOLATED -> metrics.incrementFailure();
        case HALF_OPEN -> break_(context);
      }
    } finally {
      lock.unlock();
    }
  }
}
