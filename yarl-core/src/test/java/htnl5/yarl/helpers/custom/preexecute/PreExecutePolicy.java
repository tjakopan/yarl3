package htnl5.yarl.helpers.custom.preexecute;

import htnl5.yarl.Context;
import htnl5.yarl.Policy;
import htnl5.yarl.functions.CheckedFunction;

public final class PreExecutePolicy<R> extends Policy<R, PreExecutePolicyBuilder<R>> {
  private final Runnable preExecute;

  PreExecutePolicy(final PreExecutePolicyBuilder<R> policyBuilder) {
    super(policyBuilder);
    this.preExecute = policyBuilder.getPreExecute();
  }

  public static <R> PreExecutePolicyBuilder<R> builder() {
    return new PreExecutePolicyBuilder<>();
  }

  @Override
  protected R implementation(final Context context, final CheckedFunction<Context, ? extends R> action) throws Throwable {
    return PreExecuteEngine.implementation(action, context, preExecute);
  }
}
