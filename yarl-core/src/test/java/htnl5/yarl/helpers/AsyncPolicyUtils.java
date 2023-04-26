package htnl5.yarl.helpers;

import htnl5.yarl.AsyncPolicy;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public final class AsyncPolicyUtils {
  private AsyncPolicyUtils() {
  }

  @SafeVarargs
  public static <R> CompletableFuture<R> raiseResults(final AsyncPolicy<R, ?> policy, final R... resultsToRaise) {
    final var iterator = Stream.of(resultsToRaise).iterator();
    return policy.execute(() -> {
      if (!iterator.hasNext())
        return CompletableFuture.failedFuture(new ArrayIndexOutOfBoundsException("Not enough values in resultsToRaise" +
          "."));
      return CompletableFuture.completedFuture(iterator.next());
    });
  }

  public static <R> CompletableFuture<R> raiseResultsAndOrExceptions(final AsyncPolicy<R, ?> policy,
                                                                     final Class<? extends R> resultClass,
                                                                     final Object... resultsOrExceptionsToRaise) {
    final var iterator = Stream.of(resultsOrExceptionsToRaise).iterator();
    return policy.execute(() -> {
      if (!iterator.hasNext())
        return CompletableFuture.failedFuture(new ArrayIndexOutOfBoundsException("Not enough values in " +
          "resultsOrExceptionsToRaise."));
      final var current = iterator.next();
      if (current instanceof Throwable e) return CompletableFuture.failedFuture(e);
      else if (resultClass.isInstance(current))
        //noinspection unchecked
        return CompletableFuture.completedFuture((R) current);
      else
        return CompletableFuture.failedFuture(new IllegalArgumentException("Value is not either an exception or " +
          "result."));
    });
  }
}
