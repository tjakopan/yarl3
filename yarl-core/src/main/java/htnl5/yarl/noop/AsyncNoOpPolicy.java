package htnl5.yarl.noop;

import htnl5.yarl.AsyncPolicy;
import htnl5.yarl.Context;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;

public final class AsyncNoOpPolicy<R> extends AsyncPolicy<R, AsyncNoOpPolicyBuilder<R>> implements INoOpPolicy {
  AsyncNoOpPolicy(final AsyncNoOpPolicyBuilder<R> policyBuilder) {
    super(policyBuilder);
  }

  public static <R> AsyncNoOpPolicy<R> build() {
    return new AsyncNoOpPolicyBuilder<R>().build();
  }

  @Override
  protected CompletableFuture<R> implementation(final Context context, final Executor executor,
                                                final Function<Context, ? extends CompletionStage<R>> action) {
    return action.apply(context).toCompletableFuture();
  }
}
