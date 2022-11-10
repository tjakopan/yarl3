package htnl5.yarl.circuitbreaker;

import htnl5.yarl.Context;
import htnl5.yarl.DelegateResult;
import htnl5.yarl.ExceptionPredicates;
import htnl5.yarl.ResultPredicates;
import htnl5.yarl.functions.CheckedFunction;

final class CircuitBreakerEngine {
  private CircuitBreakerEngine() {
  }

  static <R> R implementation(final CheckedFunction<Context, ? extends R> action, final Context context,
                              final ExceptionPredicates exceptionPredicates, final ResultPredicates<R> resultPredicates,
                              final ICircuitBreakerController<R> controller) throws Throwable {
    controller.onActionPreExecute();

    final var outcome = DelegateResult.runCatching(exceptionPredicates, () -> (R) action.apply(context));
    switch (outcome) {
      case DelegateResult.Success<R> s -> {
        if (resultPredicates.anyMatch(s.getResult())) {
          controller.onActionFailure(outcome, context);
        } else {
          controller.onActionSuccess(context);
        }
        return s.getResult();
      }
      case DelegateResult.Failure<R> f -> {
        final var handledException = exceptionPredicates.firstMatchOrEmpty(f.getException()).orElse(null);
        if (handledException == null) throw f.getException();
        controller.onActionFailure(outcome, context);
        throw handledException;
      }
      default -> throw new IllegalStateException("Unexpected value: " + outcome);
    }
  }
}
