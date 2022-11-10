package htnl5.yarl.fallback;

import htnl5.yarl.Context;
import htnl5.yarl.DelegateResult;
import htnl5.yarl.PolicyBuilder;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public final class FallbackPolicyBuilder<R> extends PolicyBuilder<R, FallbackPolicyBuilder<R>> {
  private final BiFunction<DelegateResult<? extends R>, Context, ? extends R> fallbackAction;

  private BiConsumer<DelegateResult<? extends R>, Context> onFallback = (outcome, ctx) -> {
  };

  public FallbackPolicyBuilder(final BiFunction<DelegateResult<? extends R>, Context, ? extends R> fallbackAction) {
    Objects.requireNonNull(fallbackAction, "fallbackAction must not be null.");
    this.fallbackAction = fallbackAction;
  }

  public FallbackPolicyBuilder(final R fallbackValue) {
    this.fallbackAction = (outcome, ctx) -> fallbackValue;
  }

  BiFunction<DelegateResult<? extends R>, Context, ? extends R> getFallbackAction() {
    return fallbackAction;
  }

  BiConsumer<DelegateResult<? extends R>, Context> getOnFallback() {
    return onFallback;
  }

  public FallbackPolicyBuilder<R> onFallback(final BiConsumer<DelegateResult<? extends R>, Context> onFallback) {
    this.onFallback = onFallback;
    return self();
  }

  @Override
  public FallbackPolicy<R> build() {
    return new FallbackPolicy<>(this);
  }

  @Override
  protected FallbackPolicyBuilder<R> self() {
    return this;
  }
}
