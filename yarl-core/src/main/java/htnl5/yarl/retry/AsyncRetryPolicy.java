package htnl5.yarl.retry;

import htnl5.yarl.AsyncEventListener;
import htnl5.yarl.AsyncPolicy;
import htnl5.yarl.Context;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;

public final class AsyncRetryPolicy<R> extends AsyncPolicy<R, AsyncRetryPolicyBuilder<R>> implements IRetryPolicy {
  private final AsyncEventListener<RetryEvent<? extends R>> onRetry;
  private final int maxRetryCount;
  private final SleepDurationProvider<R> sleepDurationProvider;
  private final SleepExecutorProvider sleepExecutorProvider;

  AsyncRetryPolicy(final AsyncRetryPolicyBuilder<R> policyBuilder) {
    super(policyBuilder);
    onRetry = policyBuilder.getOnRetry();
    maxRetryCount = policyBuilder.getMaxRetryCount();
    sleepDurationProvider = policyBuilder.getSleepDurationProvider();
    sleepExecutorProvider = policyBuilder.getSleepExecutorProvider();
  }

  public static <R> AsyncRetryPolicyBuilder<R> builder() {
    return new AsyncRetryPolicyBuilder<>();
  }

  @Override
  protected CompletableFuture<R> implementation(final Context context, final Executor executor,
                                                final Function<Context, ? extends CompletionStage<R>> action) {
    return RetryEngine.implementation(action, context, executor, exceptionPredicates, resultPredicates, onRetry,
      maxRetryCount, sleepDurationProvider, sleepExecutorProvider);
  }
}
