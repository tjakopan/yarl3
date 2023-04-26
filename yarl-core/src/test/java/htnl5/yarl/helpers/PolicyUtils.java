package htnl5.yarl.helpers;

import htnl5.yarl.Context;
import htnl5.yarl.Policy;
import htnl5.yarl.PolicyResult;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

public final class PolicyUtils {
  private PolicyUtils() {
  }

  @SafeVarargs
  public static <R> R raiseResults(final Policy<R, ?> policy, final R... resultsToRaise) throws Throwable {
    final var iterator = Stream.of(resultsToRaise).iterator();
    return policy.execute(() -> {
      if (!iterator.hasNext()) throw new ArrayIndexOutOfBoundsException("Not enough values in resultsToRaise.");
      return iterator.next();
    });
  }

  @SafeVarargs
  public static <R> R raiseResults(final Policy<R, ?> policy, final Context context, final R... resultsToRaise)
    throws Throwable {
    final var iterator = Stream.of(resultsToRaise).iterator();
    return policy.execute(context, ctx -> {
      if (!iterator.hasNext()) throw new ArrayIndexOutOfBoundsException("Not enough values in resultsToRaise.");
      return iterator.next();
    });
  }

  @SafeVarargs
  public static <R> R raiseResults(final Policy<R, ?> policy, final Map<String, Object> contextData,
                                   final R... resultsToRaise) throws Throwable {
    final var iterator = Stream.of(resultsToRaise).iterator();
    return policy.execute(contextData, ctx -> {
      if (!iterator.hasNext()) throw new ArrayIndexOutOfBoundsException("Not enough values in resultsToRaise.");
      return iterator.next();
    });
  }

  @SafeVarargs
  public static <R> PolicyResult<R> raiseResultsOnExecuteAndCapture(final Policy<R, ?> policy,
                                                                    final Map<String, Object> contextData,
                                                                    final R... resultsToRaise) {
    final var iterator = Stream.of(resultsToRaise).iterator();
    return policy.executeAndCapture(contextData, ctx -> {
      if (!iterator.hasNext()) throw new ArrayIndexOutOfBoundsException("Not enough values in resultsToRaise.");
      return iterator.next();
    });
  }

  public static <E extends Exception> void raiseExceptions(final Policy<?, ?> policy,
                                                           final int numberOfTimesToRaiseException,
                                                           final Function<Integer, ? extends E> exceptionSupplier)
    throws Throwable {
    final var counter = new AtomicInteger(0);
    policy.execute(() -> {
      counter.incrementAndGet();
      if (counter.get() <= numberOfTimesToRaiseException) throw exceptionSupplier.apply(counter.get());
      return null;
    });
  }

  public static <E extends Exception> void raiseExceptions(final Policy<?, ?> policy,
                                                           final int numberOfTimesToRaiseException,
                                                           final Class<? extends E> exceptionClass)
    throws Throwable {
    raiseExceptions(policy, numberOfTimesToRaiseException, i -> {
      try {
        return exceptionClass.getDeclaredConstructor().newInstance();
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
        throw new RuntimeException(e);
      }
    });
  }

  public static <E extends Exception> void raiseException(final Policy<?, ?> policy,
                                                          final Class<? extends E> exceptionClass)
    throws Throwable {
    policy.execute(() -> {
      throw exceptionClass.getDeclaredConstructor().newInstance();
    });
  }

  public static <E extends Exception> void raiseException(final Policy<?, ?> policy,
                                                          final Map<String, Object> contextData,
                                                          final Class<? extends E> exceptionClass)
    throws Throwable {
    policy.execute(contextData, ctx -> {
      throw exceptionClass.getDeclaredConstructor().newInstance();
    });
  }

  public static <R> R raiseResultsAndOrExceptions(final Policy<R, ?> policy, final Class<? extends R> resultClass,
                                                  final Object... resultsOrExceptionsToRaise) throws Throwable {
    final var iterator = Stream.of(resultsOrExceptionsToRaise).iterator();
    return policy.execute(() -> {
      if (!iterator.hasNext())
        throw new ArrayIndexOutOfBoundsException("Not enough values in resultsOrExceptionsToRaise.");
      final var current = iterator.next();
      if (current instanceof Throwable e) throw e;
      else if (resultClass.isInstance(current))
        //noinspection unchecked
        return (R) current;
      else throw new IllegalArgumentException("Value is not either an exception or result.");
    });
  }
}
