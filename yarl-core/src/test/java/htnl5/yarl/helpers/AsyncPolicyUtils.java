package htnl5.yarl.helpers;

import htnl5.yarl.IAsyncPolicy;
import htnl5.yarl.IReactiveAsyncPolicy;
import htnl5.yarl.PolicyResult;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;

public final class AsyncPolicyUtils {
  private AsyncPolicyUtils() {
  }

  @SafeVarargs
  public static <R> CompletableFuture<R> raiseResults(final IAsyncPolicy<R> policy, final R... resultsToRaise) {
    final var iterator = Stream.of(resultsToRaise).iterator();
    return policy.execute(() -> {
      if (!iterator.hasNext())
        return failedFuture(new ArrayIndexOutOfBoundsException("Not enough values in resultsToRaise."));
      return completedFuture(iterator.next());
    });
  }

  @SafeVarargs
  public static <R> CompletableFuture<R> raiseResults(final IAsyncPolicy<R> policy, final Runnable action,
                                                      final R... resultsToRaise) {
    final var iterator = Stream.of(resultsToRaise).iterator();
    return policy.execute(() -> {
      if (!iterator.hasNext())
        return failedFuture(new ArrayIndexOutOfBoundsException("Not enough values in resultsToRaise."));
      action.run();
      return completedFuture(iterator.next());
    });
  }

  @SafeVarargs
  public static <R> CompletableFuture<R> raiseResults(final IAsyncPolicy<R> policy,
                                                      final Supplier<? extends CompletionStage<Void>> action,
                                                      final R... resultsToRaise) {
    final var iterator = Stream.of(resultsToRaise).iterator();
    return policy.execute(() -> {
      if (!iterator.hasNext())
        return failedFuture(new ArrayIndexOutOfBoundsException("Not enough values in resultsToRaise."));
      return action.get()
        .thenApply(v -> iterator.next());
    });
  }

  @SafeVarargs
  public static <R> CompletableFuture<R> raiseResults(final IAsyncPolicy<R> policy,
                                                      final Map<String, Object> contextData,
                                                      final R... resultsToRaise) {
    final var iterator = Stream.of(resultsToRaise).iterator();
    return policy.execute(contextData, ctx -> {
      if (!iterator.hasNext())
        return failedFuture(new ArrayIndexOutOfBoundsException("Not enough values in resultsToRaise."));
      return completedFuture(iterator.next());
    });
  }

  @SafeVarargs
  public static <R> CompletableFuture<PolicyResult<R>> raiseResultsOnExecuteAndCapture(final IReactiveAsyncPolicy<R> policy,
                                                                                       final Map<String, Object> contextData,
                                                                                       final R... resultsToRaise) {
    final var iterator = Stream.of(resultsToRaise).iterator();
    return policy.executeAndCapture(contextData, ctx -> {
      if (!iterator.hasNext())
        return failedFuture(new ArrayIndexOutOfBoundsException("Not enough values in resultsToRaise."));
      return completedFuture(iterator.next());
    });
  }

  @SafeVarargs
  public static <R> CompletableFuture<R> raiseResultsAndOrCancellation(final IAsyncPolicy<R> policy,
                                                                       final int attemptDuringWhichToCancel,
                                                                       final Runnable action,
                                                                       final R... resultsToRaise) {
    final var counter = new AtomicInteger(0);
    final var iterator = Stream.of(resultsToRaise).iterator();
    final var future = new AtomicReference<CompletableFuture<R>>(null);
    final var delayedExecutor = CompletableFuture.delayedExecutor(200, TimeUnit.MILLISECONDS);
    final var delayedFuture = CompletableFuture.runAsync(() -> {
    }, delayedExecutor);
    future.set(
      policy.execute(() -> {
        if (!iterator.hasNext())
          return failedFuture(new ArrayIndexOutOfBoundsException("Not enough values in resultsToRaise."));
        return delayedFuture.thenRun(() -> {
            action.run();
            counter.incrementAndGet();
          })
          .thenComposeAsync(v -> {
            if (counter.get() >= attemptDuringWhichToCancel) {
              if (future.get() != null) future.get().cancel(false);
            }
            return completedFuture(iterator.next());
          });
      })
    );
    return future.get();
  }

  @SafeVarargs
  public static <R> CompletableFuture<R> raiseResultsAndOrCancellation(final IAsyncPolicy<R> policy,
                                                                       final int attemptDuringWhichToCancel,
                                                                       final Supplier<? extends CompletionStage<Void>> action,
                                                                       final R... resultsToRaise) {
    final var counter = new AtomicInteger(0);
    final var iterator = Stream.of(resultsToRaise).iterator();
    final var future = new AtomicReference<CompletableFuture<R>>(null);
    final var delayedExecutor = CompletableFuture.delayedExecutor(200, TimeUnit.MILLISECONDS);
    final var delayedFuture = CompletableFuture.runAsync(() -> {
    }, delayedExecutor);
    future.set(
      policy.execute(() -> {
        if (!iterator.hasNext())
          return failedFuture(new ArrayIndexOutOfBoundsException("Not enough values in resultsToRaise."));
        return delayedFuture.thenComposeAsync(v -> action.get())
          .thenComposeAsync(v -> {
            counter.incrementAndGet();
            if (counter.get() >= attemptDuringWhichToCancel) {
              if (future.get() != null) future.get().cancel(false);
            }
            return completedFuture(iterator.next());
          });
      })
    );
    return future.get();
  }

  public static <E extends Exception> CompletableFuture<?> raiseExceptions(final IAsyncPolicy<?> policy,
                                                                           final int numberOfTimesToRaiseException,
                                                                           final Function<Integer, ? extends E> exceptionSupplier) {
    final var counter = new AtomicInteger(0);
    return policy.execute(() -> {
      counter.incrementAndGet();
      if (counter.get() <= numberOfTimesToRaiseException)
        return failedFuture(exceptionSupplier.apply(counter.get()));
      return completedFuture(null);
    });
  }

  public static <E extends Exception> CompletableFuture<?> raiseExceptions(final IAsyncPolicy<?> policy,
                                                                           final int numberOfTimesToRaiseException,
                                                                           final Class<? extends E> exceptionClass) {
    return raiseExceptions(policy, numberOfTimesToRaiseException, i -> {
      try {
        return exceptionClass.getDeclaredConstructor().newInstance();
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
        throw new RuntimeException(e);
      }
    });
  }

  public static <E extends Exception> CompletableFuture<?> raiseException(final IAsyncPolicy<?> policy,
                                                                          final Class<? extends E> exceptionClass) {
    return raiseExceptions(policy, 1, exceptionClass);
  }

  public static <R> CompletableFuture<R> raiseResultsAndOrExceptions(final IAsyncPolicy<R> policy,
                                                                     final Class<? extends R> resultClass,
                                                                     final Object... resultsOrExceptionsToRaise) {
    final var iterator = Stream.of(resultsOrExceptionsToRaise).iterator();
    return policy.execute(() -> {
      if (!iterator.hasNext())
        return failedFuture(new ArrayIndexOutOfBoundsException("Not enough values in resultsOrExceptionsToRaise."));
      final var current = iterator.next();
      if (current instanceof Throwable e) return failedFuture(e);
      else if (resultClass.isInstance(current))
        //noinspection unchecked
        return completedFuture((R) current);
      else
        return failedFuture(new IllegalArgumentException("Value is not either an exception or result."));
    });
  }
}
