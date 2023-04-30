package htnl5.yarl.helpers.custom.preexecute;

import htnl5.yarl.Context;
import htnl5.yarl.ISyncPolicy;
import htnl5.yarl.Policy;
import htnl5.yarl.functions.ThrowingFunction;

public final class PreExecutePolicy<R> extends Policy<PreExecutePolicyBuilder<R>> implements ISyncPolicy<R> {
  private final Runnable preExecute;

  PreExecutePolicy(final PreExecutePolicyBuilder<R> policyBuilder) {
    super(policyBuilder);
    this.preExecute = policyBuilder.getPreExecute();
  }

  public static <R> PreExecutePolicyBuilder<R> builder() {
    return new PreExecutePolicyBuilder<>();
  }

  @Override
  public R implementation(final Context context, final ThrowingFunction<Context, ? extends R> action) throws Throwable {
    return PreExecuteEngine.implementation(action, context, preExecute);
  }
}
