package htnl5.yarl.circuitbreaker;

import htnl5.yarl.Context;
import htnl5.yarl.DelegateResult;
import htnl5.yarl.EventListener;

import java.time.Clock;
import java.time.Duration;
import java.util.function.Consumer;

class ConsecutiveCountCircuitBreakerController<R> extends CircuitBreakerStateController<R> {
  private final int failuresAllowedBeforeBreaking;

  private int consecutiveFailureCount;

  ConsecutiveCountCircuitBreakerController(int failuresAllowedBeforeBreaking, final Duration durationOfBreak,
                                           final Clock clock, final EventListener<BreakEvent<? extends R>> onBreak,
                                           final Consumer<Context> onReset, final Runnable onHalfOpen) {
    super(durationOfBreak, clock, onBreak, onReset, onHalfOpen);
    this.failuresAllowedBeforeBreaking = failuresAllowedBeforeBreaking;
    consecutiveFailureCount = 0;
  }

  @Override
  protected void resetSpecific() {
    lock.lock();
    try {
      consecutiveFailureCount = 0;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void onActionSuccess(final Context context) {
    lock.lock();
    try {
      switch (state) {
        case CLOSED -> consecutiveFailureCount = 0;
        case OPEN, ISOLATED -> {
        }
        case HALF_OPEN -> reset(context);
      }
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
          consecutiveFailureCount += 1;
          if (consecutiveFailureCount >= failuresAllowedBeforeBreaking) {
            break_(context);
          }
        }
        case OPEN, ISOLATED -> {
        }
        case HALF_OPEN -> break_(context);
      }
    } finally {
      lock.unlock();
    }
  }
}
