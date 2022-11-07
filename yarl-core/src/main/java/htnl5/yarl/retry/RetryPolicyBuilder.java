package htnl5.yarl.retry;

import java.util.Objects;

public final class RetryPolicyBuilder<R> extends RetryPolicyBuilderBase<R, RetryPolicyBuilder<R>> {
  private OnRetryListener<R> onRetry = event -> {
  };
  private Sleeper sleeper = new Sleeper() {
  };

  OnRetryListener<R> getOnRetry() {
    return onRetry;
  }

  public RetryPolicyBuilder<R> onRetry(final OnRetryListener<? super R> onRetry) {
    Objects.requireNonNull(onRetry, "onRetry must not be null.");
    this.onRetry = onRetry::accept;
    return self();
  }

  Sleeper getSleeper() {
    return sleeper;
  }

  RetryPolicyBuilder<R> sleeper(final Sleeper sleeper) {
    Objects.requireNonNull(sleeper, "sleeper must not be null.");
    this.sleeper = sleeper;
    return self();
  }

  @Override
  public RetryPolicy<R> build() {
    return new RetryPolicy<>(this);
  }

  @Override
  protected RetryPolicyBuilder<R> self() {
    return this;
  }
}
