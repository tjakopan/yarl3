package htnl5.yarl.noop;

import htnl5.yarl.IBuildable;
import htnl5.yarl.PolicyBuilder;

public final class NoOpPolicyBuilder<R> extends PolicyBuilder<NoOpPolicyBuilder<R>> implements IBuildable<NoOpPolicy<R>> {
  @Override
  public NoOpPolicyBuilder<R> self() {
    return this;
  }

  public NoOpPolicy<R> build() {
    return new NoOpPolicy<>(this);
  }
}
