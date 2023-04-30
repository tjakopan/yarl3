package htnl5.yarl.helpers.custom.addbehaviourifhandle;

import htnl5.yarl.*;
import htnl5.yarl.functions.ThrowingFunction;

import java.util.function.Consumer;

public final class AddBehaviourIfHandlePolicy<R>
  extends Policy<AddBehaviourIfHandlePolicyBuilder<R>>
  implements IReactiveSyncPolicy<R> {
  private final ResultPredicates<R> resultPredicates;
  private final ExceptionPredicates exceptionPredicates;
  private final Consumer<DelegateResult<? extends R>> behaviour;

  AddBehaviourIfHandlePolicy(final AddBehaviourIfHandlePolicyBuilder<R> policyBuilder,
                             final Consumer<DelegateResult<? extends R>> behaviour) {
    super(policyBuilder);
    this.resultPredicates = policyBuilder.getResultPredicates();
    this.exceptionPredicates = policyBuilder.getExceptionPredicates();
    this.behaviour = behaviour;
  }

  public static <R> AddBehaviourIfHandlePolicyBuilder<R> builder() {
    return new AddBehaviourIfHandlePolicyBuilder<>();
  }

  @Override
  public ResultPredicates<R> getResultPredicates() {
    return resultPredicates;
  }

  @Override
  public ExceptionPredicates getExceptionPredicates() {
    return exceptionPredicates;
  }

  @Override
  public R implementation(final Context context, final ThrowingFunction<Context, ? extends R> action) throws Throwable {
    return AddBehaviourIfHandleEngine.implementation(action, context, getExceptionPredicates(), getResultPredicates(),
      behaviour);
  }
}
