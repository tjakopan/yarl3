package htnl5.yarl.noop;

import htnl5.yarl.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;

public final class AsyncNoOpPolicy<R> extends Policy<AsyncNoOpPolicyBuilder<R>> implements IAsyncPolicy<R>,
  IReactiveAsyncPolicy<R> {
  AsyncNoOpPolicy(final AsyncNoOpPolicyBuilder<R> policyBuilder) {
    super(policyBuilder);
  }

  public static <R> AsyncNoOpPolicy<R> build() {
    return new AsyncNoOpPolicyBuilder<R>().build();
  }

  @Override
  public Executor getExecutor() {
    return DEFAULT_EXECUTOR;
  }

  @Override
  public ResultPredicates<R> getResultPredicates() {
    return ResultPredicates.none();
  }

  @Override
  public ExceptionPredicates getExceptionPredicates() {
    return ExceptionPredicates.none();
  }

  @Override
  public CompletableFuture<R> implementation(final Context context, final Executor executor,
                                             final Function<Context, ? extends CompletionStage<R>> action) {
    return action.apply(context).toCompletableFuture();
  }
}
