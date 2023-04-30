package htnl5.yarl.retry;

import htnl5.yarl.ExceptionPredicates;
import htnl5.yarl.IReactivePolicyBuilder;
import htnl5.yarl.PolicyBuilder;
import htnl5.yarl.ResultPredicates;

import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.StreamSupport;

public abstract class RetryPolicyBuilderBase<R, B extends RetryPolicyBuilderBase<R, B>>
  extends PolicyBuilder<B>
  implements IReactivePolicyBuilder<R, B> {
  private final ResultPredicates<R> resultPredicates = ResultPredicates.none();
  private final ExceptionPredicates exceptionPredicates = ExceptionPredicates.none();
  private int maxRetryCount = 3;
  private SleepDurationProvider<R> sleepDurationProvider = event -> Duration.ZERO;

  @Override
  public ResultPredicates<R> getResultPredicates() {
    return resultPredicates;
  }

  @Override
  public ExceptionPredicates getExceptionPredicates() {
    return exceptionPredicates;
  }

  int getMaxRetryCount() {
    return maxRetryCount;
  }

  public B maxRetryCount(final int maxRetryCount) {
    if (maxRetryCount < 0) throw new IllegalArgumentException("Retry count must be greater than or equal to zero.");
    this.maxRetryCount = maxRetryCount;
    return self();
  }

  public B retryForever() {
    return maxRetryCount(Integer.MAX_VALUE);
  }

  SleepDurationProvider<R> getSleepDurationProvider() {
    return sleepDurationProvider;
  }

  public B sleepDurations(final Iterable<Duration> sleepDurations) {
    Objects.requireNonNull(sleepDurations, "sleepDurations must not be null.");
    maxRetryCount = Math.toIntExact(StreamSupport.stream(sleepDurations.spliterator(), false).count());
    final var sleepDurationsIterator = sleepDurations.iterator();
    sleepDurationProvider = event -> {
      if (sleepDurationsIterator.hasNext()) return sleepDurationsIterator.next();
      else return Duration.ZERO;
    };
    return self();
  }

  public B sleepDurations(final Duration... sleepDurations) {
    maxRetryCount = sleepDurations.length;
    final var sleepDurationsIterator = Arrays.stream(sleepDurations).iterator();
    sleepDurationProvider = event -> {
      if (sleepDurationsIterator.hasNext()) return sleepDurationsIterator.next();
      else return Duration.ZERO;
    };
    return self();
  }

  public B sleepDurationProvider(final SleepDurationProvider<? super R> sleepDurationProvider) {
    Objects.requireNonNull(sleepDurationProvider, "sleepDurationProvider must not be null.");
    this.sleepDurationProvider = sleepDurationProvider::apply;
    return self();
  }
}
