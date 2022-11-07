package htnl5.yarl;

import htnl5.yarl.functions.CheckedFunction;
import htnl5.yarl.functions.CheckedSupplier;

import java.util.Map;
import java.util.Objects;

public interface ISyncPolicy<R> extends IPolicy {
  default R execute(final CheckedSupplier<? extends R> action) throws Throwable {
    return execute(Context.none(), ctx -> action.get());
  }

  default R execute(final Map<String, Object> contextData, final CheckedFunction<Context, ? extends R> action)
    throws Throwable {
    Objects.requireNonNull(contextData, "contextData must not be null.");
    return execute(new Context(contextData), action);
  }

  R execute(final Context context, final CheckedFunction<Context, ? extends R> action) throws Throwable;

  default PolicyResult<R> executeAndCapture(final CheckedSupplier<? extends R> action) {
    return executeAndCapture(Context.none(), ctx -> action.get());
  }

  default PolicyResult<R> executeAndCapture(final Map<String, Object> contextData,
                                            final CheckedFunction<Context, ? extends R> action) {
    Objects.requireNonNull(contextData, "contextData must not be null.");
    return executeAndCapture(new Context(contextData), action);
  }

  PolicyResult<R> executeAndCapture(final Context context, final CheckedFunction<Context, ? extends R> action);
}
