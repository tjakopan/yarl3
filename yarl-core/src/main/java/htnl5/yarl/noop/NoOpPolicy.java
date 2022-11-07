package htnl5.yarl.noop;

import htnl5.yarl.Context;
import htnl5.yarl.Policy;
import htnl5.yarl.functions.CheckedFunction;

public final class NoOpPolicy<R> extends Policy<R, NoOpPolicyBuilder<R>> implements INoOpPolicy {
  NoOpPolicy(final NoOpPolicyBuilder<R> policyBuilder) {
    super(policyBuilder);
  }

  public static <R> NoOpPolicyBuilder<R> builder() {
    return new NoOpPolicyBuilder<>();
  }

  @Override
  protected R implementation(final Context context, final CheckedFunction<Context, ? extends R> action)
    throws Throwable {
    return action.apply(context);
  }
}
