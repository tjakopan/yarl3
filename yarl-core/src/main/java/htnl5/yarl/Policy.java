package htnl5.yarl;

import htnl5.yarl.functions.ThrowingFunction;

import java.util.Objects;

public abstract class Policy<R, B extends PolicyBuilderBase<R, B>> extends PolicyBase<R, B> implements ISyncPolicy<R> {
  protected Policy(final PolicyBuilderBase<R, B> policyBuilder) {
    super(policyBuilder);
  }

  @Override
  public R execute(final Context context, final ThrowingFunction<Context, ? extends R> action) throws Throwable {
    Objects.requireNonNull(context, "context must not be null.");
    final var priorPolicyKey = context.getPolicyKey().orElse(null);
    context.setPolicyKey(policyKey);
    try {
      return implementation(context, action);
    } finally {
      context.setPolicyKey(priorPolicyKey);
    }
  }

  @Override
  public PolicyResult<R> executeAndCapture(final Context context, final ThrowingFunction<Context, ? extends R> action) {
    Objects.requireNonNull(context, "context must not be null.");
    try {
      final var result = execute(context, action);
      if (resultPredicates.anyMatch(result)) return PolicyResult.failureWithResult(result, context);
      else return PolicyResult.success(result, context);
    } catch (final Throwable e) {
      return PolicyResult.failureWithException(e, ExceptionType.getExceptionType(exceptionPredicates, e), context);
    }
  }

  protected abstract R implementation(final Context context, final ThrowingFunction<Context, ? extends R> action)
    throws Throwable;
}
