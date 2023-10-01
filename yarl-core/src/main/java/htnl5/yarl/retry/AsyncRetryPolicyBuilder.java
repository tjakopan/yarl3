package htnl5.yarl.retry;

import htnl5.yarl.*;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class AsyncRetryPolicyBuilder<R>
  extends RetryPolicyBuilderBase<R, AsyncRetryPolicyBuilder<R>>
  implements IAsyncPolicyBuilder<AsyncRetryPolicyBuilder<R>>, IBuildable<AsyncRetryPolicy<R>> {
  private Executor executor = IAsyncPolicy.DEFAULT_EXECUTOR;
  private AsyncEventListener<RetryEvent<? extends R>> onRetry = event -> CompletableFuture.completedFuture(null);
  private SleepExecutorProvider sleepExecutorProvider = new SleepExecutorProvider() {
  };

  @Override
  public Executor getExecutor() {
    return executor;
  }

  @Override
  public AsyncRetryPolicyBuilder<R> executor(final Executor executor) {
    Objects.requireNonNull(executor, "executor must not be null.");
    this.executor = executor;
    return this;
  }

  AsyncEventListener<RetryEvent<? extends R>> getOnRetry() {
    return onRetry;
  }

  public AsyncRetryPolicyBuilder<R> onRetryAsync(final AsyncEventListener<RetryEvent<? extends R>> onRetry) {
    Objects.requireNonNull(onRetry, "onRetryAsync must not be null.");
    this.onRetry = onRetry;
    return this;
  }

  public AsyncRetryPolicyBuilder<R> onRetry(final EventListener<RetryEvent<? extends R>> onRetry) {
    Objects.requireNonNull(onRetry, "onRetry must not be null.");
    return onRetryAsync(ev -> {
      onRetry.accept(ev);
      return CompletableFuture.completedFuture(null);
    });
  }

  SleepExecutorProvider getSleepExecutorProvider() {
    return sleepExecutorProvider;
  }

  AsyncRetryPolicyBuilder<R> sleepExecutorProvider(final SleepExecutorProvider sleepExecutorProvider) {
    Objects.requireNonNull(sleepExecutorProvider, "sleepExecutorProvider must not be null.");
    this.sleepExecutorProvider = sleepExecutorProvider;
    return this;
  }

  @Override
  public AsyncRetryPolicy<R> build() {
    return new AsyncRetryPolicy<>(this);
  }

  @Override
  public AsyncRetryPolicyBuilder<R> self() {
    return this;
  }
}
