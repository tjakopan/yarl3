package htnl5.yarl;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

public interface IAsyncPolicy<R> extends IPolicy {
  Executor DEFAULT_EXECUTOR = CompletableFuture.completedFuture(null).defaultExecutor();

  Executor getExecutor();

  default CompletableFuture<R> execute(final Supplier<? extends CompletionStage<R>> action) {
    return execute(Context.none(), context -> action.get());
  }

  default CompletableFuture<R> execute(final Map<String, Object> contextData,
                                       final Function<Context, ? extends CompletionStage<R>> action) {
    return execute(new Context(contextData), action);
  }

  default CompletableFuture<R> execute(final Context context,
                                       final Function<Context, ? extends CompletionStage<R>> action) {
    Objects.requireNonNull(context, "context must not be null.");
    final var priorPolicyKey = context.getPolicyKey().orElse(null);
    context.setPolicyKey(getPolicyKey());
    final var future = implementation(context, getExecutor(), action);
    future.whenComplete((r, e) -> context.setPolicyKey(priorPolicyKey));
    return future;
  }

  CompletableFuture<R> implementation(final Context context, final Executor executor,
                                      final Function<Context, ? extends CompletionStage<R>> action);
}
