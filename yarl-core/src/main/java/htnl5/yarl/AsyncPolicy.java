package htnl5.yarl;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;

public abstract class AsyncPolicy<R, B extends PolicyBuilderBase<R, B>>
  extends PolicyBase<R, B>
  implements IAsyncPolicy<R> {
  protected AsyncPolicy(final PolicyBuilderBase<R, B> policyBuilder) {
    super(policyBuilder);
  }

  @Override
  public CompletableFuture<R> execute(final Context context, final Executor executor,
                                      final Function<Context, ? extends CompletionStage<R>> action) {
    Objects.requireNonNull(context, "context must not be null.");
    final var priorPolicyKey = context.getPolicyKey().orElse(null);
    context.setPolicyKey(policyKey);
    return implementation(context, executor, action)
      .whenComplete((r, e) -> context.setPolicyKey(priorPolicyKey));
  }

  @Override
  public CompletableFuture<PolicyResult<R>> executeAndCapture(final Context context, final Executor executor,
                                                              final Function<Context, ? extends CompletionStage<R>> action) {
    Objects.requireNonNull(context, "context must not be null.");
    return execute(context, executor, action)
      .handle((r, e) -> {
        if (e != null) {
          return PolicyResult.failureWithException(e, ExceptionType.getExceptionType(exceptionPredicates, e), context);
        }
        if (resultPredicates.anyMatch(r)) {
          return PolicyResult.failureWithResult(r, context);
        }
        return PolicyResult.success(r, context);
      });
  }

  protected abstract CompletableFuture<R> implementation(final Context context, final Executor executor,
                                                         final Function<Context, ? extends CompletionStage<R>> action);
}
