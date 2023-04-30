package htnl5.yarl.noop;

import htnl5.yarl.*;
import htnl5.yarl.functions.ThrowingFunction;

public final class NoOpPolicy<R> extends Policy<NoOpPolicyBuilder<R>> implements IReactiveSyncPolicy<R> {
  NoOpPolicy(final NoOpPolicyBuilder<R> policyBuilder) {
    super(policyBuilder);
  }

  @Override
  public ResultPredicates<R> getResultPredicates() {
    return ResultPredicates.none();
  }

  @Override
  public ExceptionPredicates getExceptionPredicates() {
    return ExceptionPredicates.none();
  }

  public static <R> NoOpPolicy<R> build() {
    return new NoOpPolicyBuilder<R>().build();
  }

  public static <R> NoOpPolicy<R> build(final String policyKey) {
    return new NoOpPolicyBuilder<R>()
      .policyKey(policyKey)
      .build();
  }


  @Override
  public R implementation(final Context context, final ThrowingFunction<Context, ? extends R> action)
    throws Throwable {
    return action.apply(context);
  }
}
