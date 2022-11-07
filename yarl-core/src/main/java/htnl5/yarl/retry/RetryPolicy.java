package htnl5.yarl.retry;

import htnl5.yarl.Context;
import htnl5.yarl.Policy;
import htnl5.yarl.functions.CheckedFunction;

public final class RetryPolicy<R> extends Policy<R, RetryPolicyBuilder<R>> implements IRetryPolicy {
  private final OnRetryListener<R> onRetry;
  private final int maxRetryCount;
  private final SleepDurationProvider<R> sleepDurationProvider;
  private final Sleeper sleeper;

  RetryPolicy(final RetryPolicyBuilder<R> policyBuilder) {
    super(policyBuilder);
    onRetry = policyBuilder.getOnRetry();
    maxRetryCount = policyBuilder.getMaxRetryCount();
    sleepDurationProvider = policyBuilder.getSleepDurationProvider();
    sleeper = policyBuilder.getSleeper();
  }

  public static <R> RetryPolicyBuilder<R> builder() {
    return new RetryPolicyBuilder<>();
  }

  @Override
  protected R implementation(final Context context, final CheckedFunction<Context, ? extends R> action)
    throws Throwable {
    return RetryEngine.implementation(action, context, exceptionPredicates, resultPredicates, onRetry, maxRetryCount,
      sleepDurationProvider, sleeper);
  }
}
