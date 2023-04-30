package htnl5.yarl.retry;

import htnl5.yarl.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;

public final class AsyncRetryPolicy<R>
  extends Policy<AsyncRetryPolicyBuilder<R>>
  implements IAsyncPolicy<R>, IReactiveAsyncPolicy<R> {
  private final ResultPredicates<R> resultPredicates;
  private final ExceptionPredicates exceptionPredicates;
  private final Executor executor;
  private final AsyncEventListener<RetryEvent<? extends R>> onRetry;
  private final int maxRetryCount;
  private final SleepDurationProvider<R> sleepDurationProvider;
  private final SleepExecutorProvider sleepExecutorProvider;

  AsyncRetryPolicy(final AsyncRetryPolicyBuilder<R> policyBuilder) {
    super(policyBuilder);
    resultPredicates = policyBuilder.getResultPredicates();
    exceptionPredicates = policyBuilder.getExceptionPredicates();
    executor = policyBuilder.getExecutor();
    onRetry = policyBuilder.getOnRetry();
    maxRetryCount = policyBuilder.getMaxRetryCount();
    sleepDurationProvider = policyBuilder.getSleepDurationProvider();
    sleepExecutorProvider = policyBuilder.getSleepExecutorProvider();
  }

  public static <R> AsyncRetryPolicyBuilder<R> builder() {
    return new AsyncRetryPolicyBuilder<>();
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
  public Executor getExecutor() {
    return executor;
  }

  @Override
  public CompletableFuture<R> implementation(final Context context, final Executor executor,
                                             final Function<Context, ? extends CompletionStage<R>> action) {
    return RetryEngine.implementation(action, context, executor, exceptionPredicates, resultPredicates, onRetry,
      maxRetryCount, sleepDurationProvider, sleepExecutorProvider);
  }
}
