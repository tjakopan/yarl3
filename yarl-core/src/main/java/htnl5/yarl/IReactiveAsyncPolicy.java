package htnl5.yarl;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

public interface IReactiveAsyncPolicy<R> extends IAsyncPolicy<R> {
  ResultPredicates<R> getResultPredicates();

  ExceptionPredicates getExceptionPredicates();

  default CompletableFuture<PolicyResult<R>> executeAndCapture(final Supplier<? extends CompletionStage<R>> action) {
    return executeAndCapture(Context.none(), context -> action.get());
  }

  default CompletableFuture<PolicyResult<R>> executeAndCapture(final Map<String, Object> contextData,
                                                               final Function<Context, ? extends CompletionStage<R>> action) {
    return executeAndCapture(new Context(contextData), action);
  }

  default CompletableFuture<PolicyResult<R>> executeAndCapture(final Context context,
                                                               final Function<Context, ? extends CompletionStage<R>> action) {
    Objects.requireNonNull(context, "context must not be null.");
    return execute(context, action)
      .handle((r, e) -> {
        if (e != null) {
          return PolicyResult.failureWithException(e, ExceptionType.getExceptionType(getExceptionPredicates(), e),
            context);
        }
        if (getResultPredicates().anyMatch(r)) {
          return PolicyResult.failureWithResult(r, context);
        }
        return PolicyResult.success(r, context);
      });
  }
}
