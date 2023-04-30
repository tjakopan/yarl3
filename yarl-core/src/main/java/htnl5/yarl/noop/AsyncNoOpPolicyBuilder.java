package htnl5.yarl.noop;

import htnl5.yarl.IBuildable;
import htnl5.yarl.PolicyBuilder;

public final class AsyncNoOpPolicyBuilder<R>
  extends PolicyBuilder<AsyncNoOpPolicyBuilder<R>>
  implements IBuildable<AsyncNoOpPolicy<R>> {
  @Override
  public AsyncNoOpPolicyBuilder<R> self() {
    return this;
  }

  public AsyncNoOpPolicy<R> build() {
    return new AsyncNoOpPolicy<>(this);
  }
}
