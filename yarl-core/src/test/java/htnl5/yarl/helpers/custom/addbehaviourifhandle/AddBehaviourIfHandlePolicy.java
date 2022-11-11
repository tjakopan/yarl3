package htnl5.yarl.helpers.custom.addbehaviourifhandle;

import htnl5.yarl.Context;
import htnl5.yarl.DelegateResult;
import htnl5.yarl.Policy;
import htnl5.yarl.functions.CheckedFunction;

import java.util.function.Consumer;

public final class AddBehaviourIfHandlePolicy<R> extends Policy<R, AddBehaviourIfHandlePolicyBuilder<R>> {
  private final Consumer<DelegateResult<? extends R>> behaviour;

  AddBehaviourIfHandlePolicy(final AddBehaviourIfHandlePolicyBuilder<R> policyBuilder,
                             final Consumer<DelegateResult<? extends R>> behaviour) {
    super(policyBuilder);
    this.behaviour = behaviour;
  }

  public static <R> AddBehaviourIfHandlePolicyBuilder<R> builder() {
    return new AddBehaviourIfHandlePolicyBuilder<>();
  }

  @Override
  protected R implementation(final Context context, final CheckedFunction<Context, ? extends R> action) throws Throwable {
    return AddBehaviourIfHandleEngine.implementation(action, context, getExceptionPredicates(), getResultPredicates(),
      behaviour);
  }
}
