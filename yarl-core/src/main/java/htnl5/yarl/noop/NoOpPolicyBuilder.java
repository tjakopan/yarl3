package htnl5.yarl.noop;

import htnl5.yarl.PolicyBuilderBase;

public final class NoOpPolicyBuilder<R> extends PolicyBuilderBase<R, NoOpPolicyBuilder<R>> {
  @Override
  protected NoOpPolicyBuilder<R> self() {
    return this;
  }

  public NoOpPolicy<R> build() {
    return new NoOpPolicy<>(this);
  }
}
