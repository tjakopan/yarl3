package htnl5.yarl.retry;

import htnl5.yarl.*;
import htnl5.yarl.functions.CheckedFunction;

final class RetryEngine {
  private RetryEngine() {
  }

  private static <R> boolean shouldHandleResult(final R result, final ResultPredicates<R> resultPredicates) {
    return resultPredicates.anyMatch(result);
  }

  private static boolean shouldHandleException(final Throwable e, final ExceptionPredicates exceptionPredicates) {
    return exceptionPredicates.firstMatchOrEmpty(e).isPresent();
  }

  static <R> R implementation(final CheckedFunction<Context, ? extends R> action, final Context context,
                              final ExceptionPredicates exceptionPredicates, final ResultPredicates<R> resultPredicates,
                              final EventListener<RetryEvent<? extends R>> onRetry, final int maxRetryCount,
                              final SleepDurationProvider<? super R> sleepDurationProvider, final Sleeper sleeper)
    throws Throwable {
    var tryCount = 0;

    while (true) {
      final var outcome =
        DelegateResult.runCatching(exceptionPredicates, () -> (R) action.apply(context));
      switch (outcome) {
        case DelegateResult.Success<R> s -> {
          if (!shouldHandleResult(s.getResult(), resultPredicates)) {
            return s.getResult();
          }
        }
        case DelegateResult.Failure<R> f -> {
          if (!shouldHandleException(f.getException(), exceptionPredicates)) {
            throw f.getException();
          }
        }
        default -> throw new IllegalStateException("Unexpected value: " + outcome);
      }

      final var canRetry = tryCount < maxRetryCount;
      if (!canRetry) {
        return outcome.getOrThrow();
      }

      tryCount++;

      final var sleepDuration =
        sleepDurationProvider.apply(new SleepDurationEvent<>(tryCount, outcome, context));
      onRetry.accept(new RetryEvent<>(outcome, sleepDuration, tryCount, context));
      if (sleepDuration.isPositive()) {
        sleeper.sleep(sleepDuration);
      }
    }
  }
}
