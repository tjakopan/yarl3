package htnl5.yarl.helpers.custom.addbehaviourifhandle;

import htnl5.yarl.Context;
import htnl5.yarl.DelegateResult;
import htnl5.yarl.ExceptionPredicates;
import htnl5.yarl.ResultPredicates;
import htnl5.yarl.functions.CheckedFunction;

import java.util.function.Consumer;

final class AddBehaviourIfHandleEngine {
  private AddBehaviourIfHandleEngine() {
  }

  static <R> R implementation(final CheckedFunction<Context, ? extends R> action, final Context context,
                              final ExceptionPredicates exceptionPredicates, final ResultPredicates<R> resultPredicates,
                              final Consumer<DelegateResult<? extends R>> behaviour) throws Throwable {
    final var outcome = DelegateResult.runCatching(exceptionPredicates, () -> (R) action.apply(context));
    switch (outcome) {
      case DelegateResult.Success<R> s -> {
        if (resultPredicates.anyMatch(s.getResult())) {
          behaviour.accept(outcome);
        }
        return s.getResult();
      }
      case DelegateResult.Failure<R> f -> {
        final var handledException = exceptionPredicates.firstMatchOrEmpty(f.getException()).orElse(null);
        if (handledException == null) throw f.getException();
        behaviour.accept(outcome);
        throw handledException;
      }
      default -> throw new IllegalStateException("Unexpected value: " + outcome);
    }
  }
}
