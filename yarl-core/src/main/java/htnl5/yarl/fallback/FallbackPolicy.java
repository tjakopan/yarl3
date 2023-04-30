package htnl5.yarl.fallback;

import htnl5.yarl.*;
import htnl5.yarl.functions.ThrowingFunction;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public final class FallbackPolicy<R> extends Policy<FallbackPolicyBuilder<R>> implements IReactiveSyncPolicy<R> {
  private final ResultPredicates<R> resultPredicates;
  private final ExceptionPredicates exceptionPredicates;
  private final BiFunction<DelegateResult<? extends R>, Context, ? extends R> fallbackAction;
  private final BiConsumer<DelegateResult<? extends R>, Context> onFallback;

  FallbackPolicy(final FallbackPolicyBuilder<R> policyBuilder) {
    super(policyBuilder);
    this.resultPredicates = policyBuilder.getResultPredicates();
    this.exceptionPredicates = policyBuilder.getExceptionPredicates();
    this.fallbackAction = policyBuilder.getFallback();
    this.onFallback = policyBuilder.getOnFallback();
  }

  public static <R> FallbackPolicyBuilder<R> builder() {
    return new FallbackPolicyBuilder<>();
  }

  @Override
  public ResultPredicates<R> getResultPredicates() {
    return resultPredicates;
  }

  @Override
  public ExceptionPredicates getExceptionPredicates() {
    return exceptionPredicates;
  }

  @Override
  public R implementation(final Context context, final ThrowingFunction<Context, ? extends R> action)
    throws Throwable {
    return FallbackEngine.implementation(action, context, exceptionPredicates, resultPredicates, onFallback,
      fallbackAction);
  }
}
