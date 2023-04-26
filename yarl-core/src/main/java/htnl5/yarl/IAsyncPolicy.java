package htnl5.yarl;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

public interface IAsyncPolicy<R> extends IPolicy {
  Executor DEFAULT_EXECUTOR = CompletableFuture.completedFuture(null).defaultExecutor();

  default CompletableFuture<R> execute(final Supplier<? extends CompletionStage<R>> action) {
    return execute(Context.none(), DEFAULT_EXECUTOR, context -> action.get());
  }

  default CompletableFuture<R> execute(final Map<String, Object> contextData,
                                       final Function<Context, ? extends CompletionStage<R>> action) {
    return execute(new Context(contextData), DEFAULT_EXECUTOR, action);
  }

  default CompletableFuture<R> execute(final Context context,
                                       final Function<Context, ? extends CompletionStage<R>> action) {
    return execute(context, DEFAULT_EXECUTOR, action);
  }


  default CompletableFuture<R> execute(final Executor executor, final Supplier<? extends CompletionStage<R>> action) {
    return execute(Context.none(), executor, context -> action.get());
  }

  default CompletableFuture<R> execute(final Map<String, Object> contextData, final Executor executor,
                                       final Function<Context, ? extends CompletionStage<R>> action) {
    return execute(new Context(contextData), executor, action);
  }

  CompletableFuture<R> execute(final Context context, final Executor executor,
                               final Function<Context, ? extends CompletionStage<R>> action);

  default CompletableFuture<PolicyResult<R>> executeAndCapture(final Supplier<? extends CompletionStage<R>> action) {
    return executeAndCapture(Context.none(), DEFAULT_EXECUTOR, context -> action.get());
  }

  default CompletableFuture<PolicyResult<R>> executeAndCapture(final Map<String, Object> contextData,
                                                               final Function<Context, ? extends CompletionStage<R>> action) {
    return executeAndCapture(new Context(contextData), DEFAULT_EXECUTOR, action);
  }

  default CompletableFuture<PolicyResult<R>> executeAndCapture(final Context context,
                                                               final Function<Context, ? extends CompletionStage<R>> action) {
    return executeAndCapture(context, DEFAULT_EXECUTOR, action);
  }

  default CompletableFuture<PolicyResult<R>> executeAndCapture(final Executor executor,
                                                               final Supplier<? extends CompletionStage<R>> action) {
    return executeAndCapture(Context.none(), executor, context -> action.get());
  }

  default CompletableFuture<PolicyResult<R>> executeAndCapture(final Map<String, Object> contextData,
                                                               final Executor executor,
                                                               final Function<Context, ? extends CompletionStage<R>> action) {
    return executeAndCapture(new Context(contextData), executor, action);
  }

  CompletableFuture<PolicyResult<R>> executeAndCapture(final Context context, final Executor executor,
                                                       final Function<Context, ? extends CompletionStage<R>> action);
}
