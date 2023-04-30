package htnl5.yarl.fallback;

import htnl5.yarl.*;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public final class FallbackPolicyBuilder<R>
  extends PolicyBuilder<FallbackPolicyBuilder<R>>
  implements IReactivePolicyBuilder<R, FallbackPolicyBuilder<R>>, IBuildable<FallbackPolicy<R>> {
  private final ResultPredicates<R> resultPredicates = ResultPredicates.none();
  private final ExceptionPredicates exceptionPredicates = ExceptionPredicates.none();
  private BiFunction<DelegateResult<? extends R>, Context, ? extends R> fallback = (outcome, ctx) -> null;
  private BiConsumer<DelegateResult<? extends R>, Context> onFallback = (outcome, ctx) -> {
  };

  @Override
  public ResultPredicates<R> getResultPredicates() {
    return resultPredicates;
  }

  @Override
  public ExceptionPredicates getExceptionPredicates() {
    return exceptionPredicates;
  }

  BiFunction<DelegateResult<? extends R>, Context, ? extends R> getFallback() {
    return fallback;
  }

  public FallbackPolicyBuilder<R> fallback(final BiFunction<DelegateResult<? extends R>, Context, ? extends R> fallback) {
    Objects.requireNonNull(fallback, "fallback must not be null.");
    this.fallback = fallback;
    return self();
  }

  public FallbackPolicyBuilder<R> fallback(final R fallback) {
    this.fallback = (outcome, ctx) -> fallback;
    return self();
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
  public FallbackPolicyBuilder<R> self() {
    return this;
  }
}
