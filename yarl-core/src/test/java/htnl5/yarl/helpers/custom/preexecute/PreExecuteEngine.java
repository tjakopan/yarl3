package htnl5.yarl.helpers.custom.preexecute;

import htnl5.yarl.Context;
import htnl5.yarl.functions.ThrowingFunction;

final class PreExecuteEngine {
  private PreExecuteEngine() {
  }

  static <R> R implementation(final ThrowingFunction<Context, ? extends R> action, final Context context,
                              final Runnable preExecute) throws Throwable {
    if (preExecute != null) preExecute.run();
    return action.apply(context);
  }
}
