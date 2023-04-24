package htnl5.yarl.wrap;

import htnl5.yarl.Context;
import htnl5.yarl.ISyncPolicy;
import htnl5.yarl.Policy;
import htnl5.yarl.functions.ThrowingFunction;

public final class PolicyWrap<R> extends Policy<R, PolicyWrapBuilder<R>> implements IPolicyWrap {
  private final ISyncPolicy<R> outer;
  private final ISyncPolicy<R> inner;

  PolicyWrap(final PolicyWrapBuilder<R> policyBuilder, final ISyncPolicy<R> outer, final ISyncPolicy<R> inner) {
    super(policyBuilder);
    this.outer = outer;
    this.inner = inner;
  }

  public static <R> PolicyWrap<R> wrap(final ISyncPolicy<R> outer, final ISyncPolicy<R> inner) {
    return new PolicyWrapBuilder<R>().wrap(outer, inner);
  }

  public static <R> PolicyWrap<R> wrap(final String policyKey, final ISyncPolicy<R> outer, final ISyncPolicy<R> inner) {
    return new PolicyWrapBuilder<R>()
      .policyKey(policyKey)
      .wrap(outer, inner);
  }

  @SafeVarargs
  public static <R> PolicyWrap<R> wrap(final ISyncPolicy<R>... policies) {
    return new PolicyWrapBuilder<R>().wrap(policies);
  }

  @SafeVarargs
  public static <R> PolicyWrap<R> wrap(final String policyKey, final ISyncPolicy<R>... policies) {
    return new PolicyWrapBuilder<R>()
      .policyKey(policyKey)
      .wrap(policies);
  }

  @Override
  public ISyncPolicy<R> getOuter() {
    return outer;
  }

  @Override
  public ISyncPolicy<R> getInner() {
    return inner;
  }

  @Override
  public R execute(final Context context, final ThrowingFunction<Context, ? extends R> action) throws Throwable {
    final var priorPolicyWrapKey = context.getPolicyWrapKey().orElse(null);
    if (context.getPolicyWrapKey().isEmpty()) context.setPolicyWrapKey(getPolicyKey());
    try {
      return super.execute(context, action);
    } finally {
      context.setPolicyWrapKey(priorPolicyWrapKey);
    }
  }

  @Override
  protected R implementation(final Context context, final ThrowingFunction<Context, ? extends R> action)
    throws Throwable {
    return PolicyWrapEngine.implementation(action, context, outer, inner);
  }
}
