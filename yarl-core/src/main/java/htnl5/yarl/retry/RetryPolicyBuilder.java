package htnl5.yarl.retry;

import htnl5.yarl.EventListener;
import htnl5.yarl.IBuildable;

import java.util.Objects;

public final class RetryPolicyBuilder<R>
  extends RetryPolicyBuilderBase<R, RetryPolicyBuilder<R>>
  implements IBuildable<RetryPolicy<R>> {
  private EventListener<RetryEvent<? extends R>> onRetry = event -> {
  };
  private Sleeper sleeper = new Sleeper() {
  };

  EventListener<RetryEvent<? extends R>> getOnRetry() {
    return onRetry;
  }

  public RetryPolicyBuilder<R> onRetry(final EventListener<RetryEvent<? extends R>> onRetry) {
    Objects.requireNonNull(onRetry, "onRetry must not be null.");
    this.onRetry = onRetry;
    return this;
  }

  Sleeper getSleeper() {
    return sleeper;
  }

  RetryPolicyBuilder<R> sleeper(final Sleeper sleeper) {
    Objects.requireNonNull(sleeper, "sleeper must not be null.");
    this.sleeper = sleeper;
    return this;
  }

  @Override
  public RetryPolicy<R> build() {
    return new RetryPolicy<>(this);
  }

  @Override
  public RetryPolicyBuilder<R> self() {
    return this;
  }
}
