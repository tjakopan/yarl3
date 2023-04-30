package htnl5.yarl.helpers.custom.preexecute;

import htnl5.yarl.IBuildable;
import htnl5.yarl.PolicyBuilder;

public final class PreExecutePolicyBuilder<R>
  extends PolicyBuilder<PreExecutePolicyBuilder<R>>
  implements IBuildable<PreExecutePolicy<R>> {
  private Runnable preExecute;

  Runnable getPreExecute() {
    return preExecute;
  }

  public PreExecutePolicyBuilder<R> preExecute(final Runnable preExecute) {
    this.preExecute = preExecute;
    return self();
  }

  @Override
  public PreExecutePolicyBuilder<R> self() {
    return this;
  }

  public PreExecutePolicy<R> build() {
    return new PreExecutePolicy<>(this);
  }
}
