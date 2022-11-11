package htnl5.yarl.helpers.custom.preexecute;

import htnl5.yarl.PolicyBuilderBase;

public final class PreExecutePolicyBuilder<R> extends PolicyBuilderBase<R, PreExecutePolicyBuilder<R>> {
  private Runnable preExecute;

  Runnable getPreExecute() {
    return preExecute;
  }

  public PreExecutePolicyBuilder<R> preExecute(final Runnable preExecute) {
    this.preExecute = preExecute;
    return self();
  }

  @Override
  protected PreExecutePolicyBuilder<R> self() {
    return this;
  }

  public PreExecutePolicy<R> build() {
    return new PreExecutePolicy<>(this);
  }
}
