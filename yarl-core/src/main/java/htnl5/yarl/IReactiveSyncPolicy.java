package htnl5.yarl;

import htnl5.yarl.functions.ThrowingFunction;
import htnl5.yarl.functions.ThrowingSupplier;

import java.util.Map;
import java.util.Objects;

public interface IReactiveSyncPolicy<R> extends ISyncPolicy<R> {
  ResultPredicates<R> getResultPredicates();

  ExceptionPredicates getExceptionPredicates();

  default PolicyResult<R> executeAndCapture(final ThrowingSupplier<? extends R> action) {
    return executeAndCapture(Context.none(), ctx -> action.get());
  }

  default PolicyResult<R> executeAndCapture(final Map<String, Object> contextData,
                                            final ThrowingFunction<Context, ? extends R> action) {
    Objects.requireNonNull(contextData, "contextData must not be null.");
    return executeAndCapture(new Context(contextData), action);
  }

  default PolicyResult<R> executeAndCapture(final Context context,
                                            final ThrowingFunction<Context, ? extends R> action) {
    Objects.requireNonNull(context, "context must not be null.");
    try {
      final var result = execute(context, action);
      if (getResultPredicates().anyMatch(result)) return PolicyResult.failureWithResult(result, context);
      else return PolicyResult.success(result, context);
    } catch (final Throwable e) {
      return PolicyResult.failureWithException(e, ExceptionType.getExceptionType(getExceptionPredicates(), e), context);
    }
  }
}
