package htnl5.yarl.retry;

import htnl5.yarl.*;
import htnl5.yarl.functions.ThrowingFunction;

public final class RetryPolicy<R> extends Policy<RetryPolicyBuilder<R>> implements IReactiveSyncPolicy<R> {
  private final ResultPredicates<R> resultPredicates;
  private final ExceptionPredicates exceptionPredicates;
  private final EventListener<RetryEvent<? extends R>> onRetry;
  private final int maxRetryCount;
  private final SleepDurationProvider<R> sleepDurationProvider;
  private final Sleeper sleeper;

  RetryPolicy(final RetryPolicyBuilder<R> policyBuilder) {
    super(policyBuilder);
    resultPredicates = policyBuilder.getResultPredicates();
    exceptionPredicates = policyBuilder.getExceptionPredicates();
    onRetry = policyBuilder.getOnRetry();
    maxRetryCount = policyBuilder.getMaxRetryCount();
    sleepDurationProvider = policyBuilder.getSleepDurationProvider();
    sleeper = policyBuilder.getSleeper();
  }

  public static <R> RetryPolicyBuilder<R> builder() {
    return new RetryPolicyBuilder<>();
  }

  @Override
  public ResultPredicates<R> getResultPredicates() {
    return resultPredicates;
  }

  @Override
  public ExceptionPredicates getExceptionPredicates() {
    return exceptionPredicates;
  }

  @Override
  public R implementation(final Context context, final ThrowingFunction<Context, ? extends R> action)
    throws Throwable {
    return RetryEngine.implementation(action, context, exceptionPredicates, resultPredicates, onRetry, maxRetryCount,
      sleepDurationProvider, sleeper);
  }
}
