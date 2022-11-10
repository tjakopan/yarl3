package htnl5.yarl.fallback;

import htnl5.yarl.Context;
import htnl5.yarl.DelegateResult;
import htnl5.yarl.Policy;
import htnl5.yarl.functions.CheckedFunction;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public final class FallbackPolicy<R> extends Policy<R, FallbackPolicyBuilder<R>> implements IFallbackPolicy {
  private final BiFunction<DelegateResult<? extends R>, Context, ? extends R> fallbackAction;
  private final BiConsumer<DelegateResult<? extends R>, Context> onFallback;

  FallbackPolicy(final FallbackPolicyBuilder<R> policyBuilder) {
    super(policyBuilder);
    this.fallbackAction = policyBuilder.getFallback();
    this.onFallback = policyBuilder.getOnFallback();
  }

  public static <R> FallbackPolicyBuilder<R> builder() {
    return new FallbackPolicyBuilder<>();
  }

  @Override
  protected R implementation(final Context context, final CheckedFunction<Context, ? extends R> action)
    throws Throwable {
    return FallbackEngine.implementation(action, context, exceptionPredicates, resultPredicates, onFallback,
      fallbackAction);
  }
}
