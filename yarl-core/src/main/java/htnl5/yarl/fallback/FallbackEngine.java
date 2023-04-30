package htnl5.yarl.fallback;

import htnl5.yarl.Context;
import htnl5.yarl.DelegateResult;
import htnl5.yarl.ExceptionPredicates;
import htnl5.yarl.ResultPredicates;
import htnl5.yarl.functions.ThrowingFunction;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

final class FallbackEngine {
  private FallbackEngine() {
  }

  static <R> R implementation(final ThrowingFunction<Context, ? extends R> action, final Context context,
                              final ExceptionPredicates exceptionPredicates, final ResultPredicates<R> resultPredicates,
                              final BiConsumer<DelegateResult<? extends R>, Context> onFallback,
                              final BiFunction<DelegateResult<? extends R>, Context, ? extends R> fallbackAction)
    throws Throwable {
    final var outcome = DelegateResult.runCatching(exceptionPredicates, () -> (R) action.apply(context));
    final var shouldHandle = outcome.shouldHandle(resultPredicates, exceptionPredicates);
    if (!shouldHandle) {
      return outcome.getOrThrow();
    }

    onFallback.accept(outcome, context);

    return fallbackAction.apply(outcome, context);
  }
}
