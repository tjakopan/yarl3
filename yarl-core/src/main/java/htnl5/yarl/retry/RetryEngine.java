package htnl5.yarl.retry;

import htnl5.yarl.*;
import htnl5.yarl.functions.ThrowingFunction;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;

final class RetryEngine {
  private RetryEngine() {
  }

  private static <R> boolean shouldHandleResult(final R result, final ResultPredicates<R> resultPredicates) {
    return resultPredicates.anyMatch(result);
  }

  private static boolean shouldHandleException(final Throwable e, final ExceptionPredicates exceptionPredicates) {
    return exceptionPredicates.firstMatchOrEmpty(e).isPresent();
  }

  static <R> R implementation(final ThrowingFunction<Context, ? extends R> action, final Context context,
                              final ExceptionPredicates exceptionPredicates, final ResultPredicates<R> resultPredicates,
                              final EventListener<RetryEvent<? extends R>> onRetry, final int maxRetryCount,
                              final SleepDurationProvider<? super R> sleepDurationProvider, final Sleeper sleeper)
    throws Throwable {
    var tryCount = 0;

    while (true) {
      final var outcome =
        DelegateResult.runCatching(exceptionPredicates, () -> (R) action.apply(context));
      if (outcome instanceof DelegateResult.Success<R> s) {
        if (!shouldHandleResult(s.getResult(), resultPredicates)) {
          return s.getResult();
        }
      } else if (outcome instanceof DelegateResult.Failure<R> f) {
        if (!shouldHandleException(f.getException(), exceptionPredicates)) {
          throw f.getException();
        }
      } else {
        throw new IllegalStateException("Unexpected value: " + outcome);
      }

      final var canRetry = tryCount < maxRetryCount;
      if (!canRetry) {
        return outcome.getOrThrow();
      }

      tryCount++;

      final var sleepDuration =
        sleepDurationProvider.apply(new SleepDurationEvent<>(tryCount, outcome, context));
      onRetry.accept(new RetryEvent<>(outcome, sleepDuration, tryCount, context));
      if (!sleepDuration.isNegative() && !sleepDuration.isZero()) {
        sleeper.sleep(sleepDuration);
      }
    }
  }

  static <R> CompletableFuture<R> implementation(final Function<Context, ? extends CompletionStage<R>> action,
                                                 final Context context, final Executor executor,
                                                 final ExceptionPredicates exceptionPredicates,
                                                 final ResultPredicates<R> resultPredicates,
                                                 final AsyncEventListener<RetryEvent<? extends R>> onRetry,
                                                 final int maxRetryCount,
                                                 final SleepDurationProvider<? super R> sleepDurationProvider,
                                                 final SleepExecutorProvider sleepExecutorProvider) {
    return run(action, context, executor, exceptionPredicates, resultPredicates, onRetry, maxRetryCount,
      sleepDurationProvider, sleepExecutorProvider, 0);
  }

  private static <R> CompletableFuture<R> run(final Function<Context, ? extends CompletionStage<R>> action,
                                              final Context context, final Executor executor,
                                              final ExceptionPredicates exceptionPredicates,
                                              final ResultPredicates<R> resultPredicates,
                                              final AsyncEventListener<RetryEvent<? extends R>> onRetry,
                                              final int maxRetryCount,
                                              final SleepDurationProvider<? super R> sleepDurationProvider,
                                              final SleepExecutorProvider sleepExecutorProvider,
                                              final int tryCount) {
    return action.apply(context)
      .exceptionallyCompose(e -> CompletableFuture.failedFuture(exceptionPredicates.firstMatchOrEmpty(e).orElse(e)))
      .<CompletableFuture<R>>handleAsync((r, e) -> {
        final var outcome = DelegateResult.delegateResult(r, e);
        if (outcome instanceof DelegateResult.Success<R> s) {
          if (!shouldHandleResult(s.getResult(), resultPredicates)) {
            return CompletableFuture.completedFuture(s.getResult());
          }
        } else if (outcome instanceof DelegateResult.Failure<R> f) {
          if (!shouldHandleException(f.getException(), exceptionPredicates)) {
            return CompletableFuture.failedFuture(f.getException());
          }
        } else {
          return CompletableFuture.failedFuture(new IllegalStateException("Unexpected value: " + outcome));
        }

        final var canRetry = tryCount < maxRetryCount;
        if (!canRetry) {
          return outcome.toCompletableFuture();
        }

        final var newTryCount = tryCount + 1;

        final var sleepDuration = sleepDurationProvider.apply(new SleepDurationEvent<>(newTryCount, outcome, context));
        return onRetry.apply(new RetryEvent<>(outcome, sleepDuration, newTryCount, context))
          .thenComposeAsync(v -> sleep(sleepDuration, executor, sleepExecutorProvider), executor)
          .thenComposeAsync(v -> run(action, context, executor, exceptionPredicates, resultPredicates, onRetry,
            maxRetryCount, sleepDurationProvider, sleepExecutorProvider, newTryCount));
      }, executor)
      .thenCompose(Function.identity())
      .toCompletableFuture();
  }

  private static CompletableFuture<Void> sleep(final Duration sleepDuration, final Executor executor,
                                               final SleepExecutorProvider sleepExecutorProvider) {
    final Executor sleepExecutor = sleepExecutorProvider.sleepExecutor(sleepDuration, executor);
    return CompletableFuture.runAsync(() -> {
    }, sleepExecutor);
  }
}
