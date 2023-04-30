package htnl5.yarl.helpers.custom.addbehaviourifhandle;

import htnl5.yarl.Context;
import htnl5.yarl.DelegateResult;
import htnl5.yarl.ExceptionPredicates;
import htnl5.yarl.ResultPredicates;
import htnl5.yarl.functions.ThrowingFunction;

import java.util.function.Consumer;

final class AddBehaviourIfHandleEngine {
  private AddBehaviourIfHandleEngine() {
  }

  static <R> R implementation(final ThrowingFunction<Context, ? extends R> action, final Context context,
                              final ExceptionPredicates exceptionPredicates, final ResultPredicates<R> resultPredicates,
                              final Consumer<DelegateResult<? extends R>> behaviour) throws Throwable {
    final var outcome = DelegateResult.runCatching(exceptionPredicates, () -> (R) action.apply(context));
    final var shouldHandle = outcome.shouldHandle(resultPredicates, exceptionPredicates);
    if (shouldHandle) {
      behaviour.accept(outcome);
    }
    return outcome.getOrThrow();
  }
}
