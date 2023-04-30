package htnl5.yarl.helpers.custom.addbehaviourifhandle;

import htnl5.yarl.*;

import java.util.function.Consumer;

public final class AddBehaviourIfHandlePolicyBuilder<R>
  extends PolicyBuilder<AddBehaviourIfHandlePolicyBuilder<R>>
  implements IReactivePolicyBuilder<R, AddBehaviourIfHandlePolicyBuilder<R>>,
  IBuildable<AddBehaviourIfHandlePolicy<R>> {
  private final ResultPredicates<R> resultPredicates = ResultPredicates.none();
  private final ExceptionPredicates exceptionPredicates = ExceptionPredicates.none();
  private Consumer<DelegateResult<? extends R>> behaviour = outcome -> {
  };

  @Override
  public ResultPredicates<R> getResultPredicates() {
    return resultPredicates;
  }

  @Override
  public ExceptionPredicates getExceptionPredicates() {
    return exceptionPredicates;
  }

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
  public AddBehaviourIfHandlePolicyBuilder<R> self() {
    return this;
  }
}
