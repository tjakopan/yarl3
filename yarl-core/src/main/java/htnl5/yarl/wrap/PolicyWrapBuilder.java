package htnl5.yarl.wrap;

import htnl5.yarl.PolicyBuilderBase;

public final class PolicyWrapBuilder<R> extends PolicyBuilderBase<R, PolicyWrapBuilder<R>> {
  @Override
  protected PolicyWrapBuilder<R> self() {
    return this;
  }


}
