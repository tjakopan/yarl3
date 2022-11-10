package htnl5.yarl.circuitbreaker;

import htnl5.yarl.Context;
import htnl5.yarl.DelegateResult;
import htnl5.yarl.EventListener;

import java.time.Clock;
import java.time.Duration;
import java.util.function.Consumer;

class ConsecutiveCountCircuitBreakerController<R> extends CircuitBreakerStateController<R> {
  private final int exceptionsAllowedBeforeBreaking;

  private int consecutiveFailureCount;

  ConsecutiveCountCircuitBreakerController(int exceptionsAllowedBeforeBreaking, final Duration durationOfBreak,
                                           final Clock clock, final EventListener<BreakEvent<? extends R>> onBreak,
                                           final Consumer<Context> onReset, final Runnable onHalfOpen) {
    super(durationOfBreak, clock, onBreak, onReset, onHalfOpen);
    this.exceptionsAllowedBeforeBreaking = exceptionsAllowedBeforeBreaking;
    consecutiveFailureCount = 0;
  }

  @Override
  protected void resetSpecific() {
    consecutiveFailureCount = 0;
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
  public void onActionFailure(final DelegateResult<? extends R> outcome, final Context context) {
    lock.lock();
    try {
      switch (state) {
        case CLOSED -> {
          consecutiveFailureCount += 1;
          if (consecutiveFailureCount >= exceptionsAllowedBeforeBreaking) {
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
