package htnl5.yarl.fallback;

import htnl5.yarl.Context;
import htnl5.yarl.DelegateResult;
import htnl5.yarl.ExceptionPredicates;
import htnl5.yarl.ResultPredicates;
import htnl5.yarl.functions.CheckedFunction;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

final class FallbackEngine {
  private FallbackEngine() {
  }

  static <R> R implementation(final CheckedFunction<Context, ? extends R> action, final Context context,
                              final ExceptionPredicates exceptionPredicates, final ResultPredicates<R> resultPredicates,
                              final BiConsumer<DelegateResult<? extends R>, Context> onFallback,
                              final BiFunction<DelegateResult<? extends R>, Context, ? extends R> fallbackAction)
    throws Throwable {
    final var outcome = DelegateResult.runCatching(exceptionPredicates, () -> (R) action.apply(context));
    switch (outcome) {
      case DelegateResult.Success<R> s -> {
        if (!resultPredicates.anyMatch(s.getResult())) return s.getResult();
      }
      case DelegateResult.Failure<R> f -> {
        final var handledException = exceptionPredicates.firstMatchOrEmpty(f.getException()).orElse(null);
        if (handledException == null) throw f.getException();
      }
      default -> throw new IllegalStateException("Unexpected value: " + outcome);
    }

    onFallback.accept(outcome, context);

    return fallbackAction.apply(outcome, context);
  }
}
