package htnl5.yarl.noop;

import htnl5.yarl.PolicyBuilderBase;

public final class AsyncNoOpPolicyBuilder<R> extends PolicyBuilderBase<R, AsyncNoOpPolicyBuilder<R>> {
  @Override
  protected AsyncNoOpPolicyBuilder<R> self() {
    return this;
  }

  public AsyncNoOpPolicy<R> build() {
    return new AsyncNoOpPolicy<>(this);
  }
}
