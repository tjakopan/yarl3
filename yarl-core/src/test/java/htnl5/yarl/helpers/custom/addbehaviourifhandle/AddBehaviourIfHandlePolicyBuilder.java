package htnl5.yarl.helpers.custom.addbehaviourifhandle;

import htnl5.yarl.DelegateResult;
import htnl5.yarl.PolicyBuilder;

import java.util.function.Consumer;

public final class AddBehaviourIfHandlePolicyBuilder<R> extends PolicyBuilder<R, AddBehaviourIfHandlePolicyBuilder<R>> {
  private Consumer<DelegateResult<? extends R>> behaviour = outcome -> {
  };

  Consumer<DelegateResult<? extends R>> getBehaviour() {
    return behaviour;
  }

  public AddBehaviourIfHandlePolicyBuilder<R> behaviour(final Consumer<DelegateResult<? extends R>> behaviour) {
    this.behaviour = behaviour;
    return self();
  }

  @Override
  public AddBehaviourIfHandlePolicy<R> build() {
    return new AddBehaviourIfHandlePolicy<>(this, behaviour);
  }

  @Override
  protected AddBehaviourIfHandlePolicyBuilder<R> self() {
    return this;
  }
}
