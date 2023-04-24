package htnl5.yarl.noop;

import htnl5.yarl.Context;
import htnl5.yarl.Policy;
import htnl5.yarl.functions.ThrowingFunction;

public final class NoOpPolicy<R> extends Policy<R, NoOpPolicyBuilder<R>> implements INoOpPolicy {
  NoOpPolicy(final NoOpPolicyBuilder<R> policyBuilder) {
    super(policyBuilder);
  }

  public static <R> NoOpPolicy<R> build() {
    return new NoOpPolicyBuilder<R>().build();
  }

  public static <R> NoOpPolicy<R> build(final String policyKey) {
    return new NoOpPolicyBuilder<R>()
      .policyKey(policyKey)
      .build();
  }

  @Override
  protected R implementation(final Context context, final ThrowingFunction<Context, ? extends R> action)
    throws Throwable {
    return action.apply(context);
  }
}
