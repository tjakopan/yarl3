package htnl5.yarl.wrap;

import htnl5.yarl.Context;
import htnl5.yarl.ISyncPolicy;
import htnl5.yarl.functions.ThrowingFunction;

final class PolicyWrapEngine {
  private PolicyWrapEngine() {
  }

  static <R> R implementation(final ThrowingFunction<Context, ? extends R> action, final Context context,
                              final ISyncPolicy<R> outerPolicy, final ISyncPolicy<R> innerPolicy) throws Throwable {
    return outerPolicy.execute(context, ctx -> innerPolicy.execute(ctx, action));
  }
}
