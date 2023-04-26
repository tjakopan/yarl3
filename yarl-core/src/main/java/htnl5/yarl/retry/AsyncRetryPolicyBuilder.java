package htnl5.yarl.retry;

import htnl5.yarl.AsyncEventListener;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class AsyncRetryPolicyBuilder<R> extends RetryPolicyBuilderBase<R, AsyncRetryPolicyBuilder<R>> {
  private AsyncEventListener<RetryEvent<? extends R>> onRetry = event -> CompletableFuture.completedFuture(null);

  private SleepExecutorProvider sleepExecutorProvider = new SleepExecutorProvider() {
  };

  AsyncEventListener<RetryEvent<? extends R>> getOnRetry() {
    return onRetry;
  }

  public AsyncRetryPolicyBuilder<R> onRetry(final AsyncEventListener<RetryEvent<? extends R>> onRetry) {
    Objects.requireNonNull(onRetry, "onRetry must not be null.");
    this.onRetry = onRetry;
    return self();
  }

  SleepExecutorProvider getSleepExecutorProvider() {
    return sleepExecutorProvider;
  }

  AsyncRetryPolicyBuilder<R> sleepExecutorProvider(final SleepExecutorProvider sleepExecutorProvider) {
    Objects.requireNonNull(sleepExecutorProvider, "sleepExecutorProvider must not be null.");
    this.sleepExecutorProvider = sleepExecutorProvider;
    return self();
  }

  @Override
  public AsyncRetryPolicy<R> build() {
    return new AsyncRetryPolicy<>(this);
  }

  @Override
  protected AsyncRetryPolicyBuilder<R> self() {
    return this;
  }
}
