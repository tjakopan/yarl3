package htnl5.yarl.circuitbreaker;

import htnl5.yarl.Context;
import htnl5.yarl.DelegateResult;
import htnl5.yarl.ExceptionPredicates;
import htnl5.yarl.ResultPredicates;
import htnl5.yarl.functions.ThrowingFunction;

final class CircuitBreakerEngine {
  private CircuitBreakerEngine() {
  }

  static <R> R implementation(final ThrowingFunction<Context, ? extends R> action, final Context context,
                              final ExceptionPredicates exceptionPredicates, final ResultPredicates<R> resultPredicates,
                              final ICircuitBreakerController<R> controller) throws Throwable {
    controller.onActionPreExecute();

    final var outcome = DelegateResult.runCatching(exceptionPredicates, () -> (R) action.apply(context));
    final var shouldHandle = outcome.shouldHandle(resultPredicates, exceptionPredicates);
    if (shouldHandle) {
      outcome.onSuccess(r -> controller.onActionFailure(outcome, context))
        .onFailure(e -> controller.onActionFailure(outcome, context));
    } else {
      outcome.onSuccess(r -> controller.onActionSuccess(context));
    }
    return outcome.getOrThrow();
  }
}
