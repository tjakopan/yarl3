package htnl5.yarl.retry;

import htnl5.yarl.Context;
import htnl5.yarl.PolicyResult;
import htnl5.yarl.helpers.Result;
import htnl5.yarl.helpers.ResultClass;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static htnl5.yarl.helpers.AsyncPolicyUtils.*;
import static org.assertj.core.api.Assertions.*;

public class AsyncRetryResultHandlingTest {
  @Test
  public void shouldNotReturnHandledResultWhenHandledResultRaisedSameNumberOfTimesAsRetryCount() {
    final var policy = AsyncRetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .maxRetryCount(3)
      .build();

    final var result = raiseResults(policy, Result.FAULT, Result.FAULT, Result.FAULT, Result.GOOD)
      .join();

    assertThat(result).isEqualTo(Result.GOOD);
  }

  @Test
  public void shouldNotReturnHandledResultWhenOneOfTheHandledResultsRaisedSameNumberOfTimesAsRetryCount() {
    final var policy = AsyncRetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .handleResult(Result.FAULT_AGAIN)
      .maxRetryCount(3)
      .build();

    final var result = raiseResults(policy, Result.FAULT, Result.FAULT_AGAIN, Result.FAULT, Result.GOOD)
      .join();

    assertThat(result).isEqualTo(Result.GOOD);
  }

  @Test
  public void shouldNotReturnHandledResultWhenHandledResultRaisedLessNumberOfTimesThantRetryCount() {
    final var policy = AsyncRetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .maxRetryCount(3)
      .build();

    final var result = raiseResults(policy, Result.FAULT, Result.GOOD).join();

    assertThat(result).isEqualTo(Result.GOOD);
  }

  @Test
  public void shouldNotReturnHandledResultWhenAllOfTheHandledResultsRaisedLessNumberOfTimesThanRetryCount() {
    final var policy = AsyncRetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .handleResult(Result.FAULT_AGAIN)
      .maxRetryCount(3)
      .build();

    final var result = raiseResults(policy, Result.FAULT, Result.FAULT_AGAIN, Result.GOOD).join();

    assertThat(result).isEqualTo(Result.GOOD);
  }

  @Test
  public void shouldReturnHandledResultWhenHandledResultRaisedMoreTimesThanRetryCount() {
    final var policy = AsyncRetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .maxRetryCount(3)
      .build();

    final var result = raiseResults(policy, Result.FAULT, Result.FAULT, Result.FAULT, Result.FAULT, Result.GOOD).join();

    assertThat(result).isEqualTo(Result.FAULT);
  }

  @Test
  public void shouldReturnHandledResultWhenOneOfTheHandledResultRaisedMoreTimesThanRetryCount() {
    final var policy = AsyncRetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .handleResult(Result.FAULT_AGAIN)
      .maxRetryCount(3)
      .build();

    final var result = raiseResults(policy, Result.FAULT_AGAIN, Result.FAULT_AGAIN, Result.FAULT_AGAIN,
      Result.FAULT_AGAIN, Result.GOOD)
      .join();

    assertThat(result).isEqualTo(Result.FAULT_AGAIN);
  }

  @Test
  public void shouldReturnResultWhenResultIsNotTheSpecifiedHandledResult() {
    final var policy = AsyncRetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .build();

    final var result = raiseResults(policy, Result.FAULT_AGAIN, Result.GOOD).join();

    assertThat(result).isEqualTo(Result.FAULT_AGAIN);
  }

  @Test
  public void shouldReturnResultWhenResultIsNotOneOfTheSpecifiedHandledResults() {
    final var policy = AsyncRetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .handleResult(Result.FAULT_AGAIN)
      .build();

    final var result = raiseResults(policy, Result.FAULT_YET_AGAIN, Result.GOOD).join();

    assertThat(result).isEqualTo(Result.FAULT_YET_AGAIN);
  }

  @Test
  public void shouldReturnResultWhenSpecifiedResultPredicateIsNotSatisfied() {
    final var policy = AsyncRetryPolicy.<ResultClass>builder()
      .handleResult(r -> r.getResultCode() == Result.FAULT)
      .build();

    final var result = raiseResults(policy, new ResultClass(Result.FAULT_AGAIN), new ResultClass(Result.GOOD)).join();

    assertThat(result.getResultCode()).isEqualTo(Result.FAULT_AGAIN);
  }

  @Test
  public void shouldReturnResultWhenNoneOfTheSpecifiedResultPredicatesAreSatisfied() {
    final var policy = AsyncRetryPolicy.<ResultClass>builder()
      .handleResult(r -> r.getResultCode() == Result.FAULT)
      .handleResult(r -> r.getResultCode() == Result.FAULT_AGAIN)
      .build();

    final var result = raiseResults(policy, new ResultClass(Result.FAULT_YET_AGAIN), new ResultClass(Result.GOOD))
      .join();

    assertThat(result.getResultCode()).isEqualTo(Result.FAULT_YET_AGAIN);
  }

  @Test
  public void shouldNotReturnHandledResultWhenSpecifiedResultPredicatesIsSatisfied() {
    final var policy = AsyncRetryPolicy.<ResultClass>builder()
      .handleResult(r -> r.getResultCode() == Result.FAULT)
      .build();

    final var result = raiseResults(policy, new ResultClass(Result.FAULT), new ResultClass(Result.GOOD)).join();

    assertThat(result.getResultCode()).isEqualTo(Result.GOOD);
  }

  @Test
  public void shouldNotReturnHandledResultWhenOneOfTheSpecifiedResultPredicatesIsSatisfied() {
    final var policy = AsyncRetryPolicy.<ResultClass>builder()
      .handleResult(r -> r.getResultCode() == Result.FAULT)
      .handleResult(r -> r.getResultCode() == Result.FAULT_AGAIN)
      .build();

    final var result = raiseResults(policy, new ResultClass(Result.FAULT_AGAIN), new ResultClass(Result.GOOD)).join();

    assertThat(result.getResultCode()).isEqualTo(Result.GOOD);
  }

  @Test
  public void shouldCallOnRetryOnEachRetryWithTheCurrentTryCount() {
    final var expectedTryCounts = List.of(1, 2, 3);
    final var tryCounts = new ArrayList<Integer>(3);
    final var policy = AsyncRetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .maxRetryCount(3)
      .onRetryAsync(ev -> {
        tryCounts.add(ev.tryCount());
        return CompletableFuture.completedFuture(null);
      })
      .build();

    final var result = raiseResults(policy, Result.FAULT, Result.FAULT, Result.FAULT, Result.GOOD).join();

    assertThat(result).isEqualTo(Result.GOOD);
    assertThat(tryCounts).containsExactlyElementsOf(expectedTryCounts);
  }

  @Test
  public void shouldCallOnRetryOnEachRetryWithTheCurrentHandledResult() {
    final var expectedFaults = List.of("Fault #1", "Fault #2", "Fault #3");
    final var tryFaults = new ArrayList<String>(3);
    final var policy = AsyncRetryPolicy.<ResultClass>builder()
      .handleResult(r -> r.getResultCode() == Result.FAULT)
      .maxRetryCount(3)
      .onRetry(ev -> ev.outcome().onSuccess(r -> tryFaults.add(r.getSomeString())))
      .build();
    final var resultsToRaise = expectedFaults.stream()
      .map(s -> new ResultClass(Result.FAULT, s))
      .collect(Collectors.toList());
    resultsToRaise.add(new ResultClass(Result.FAULT));

    final var result = raiseResults(policy, resultsToRaise.toArray(ResultClass[]::new)).join();

    assertThat(result.getResultCode()).isEqualTo(Result.FAULT);
    assertThat(tryFaults).containsExactlyElementsOf(expectedFaults);
  }

  @Test
  public void shouldNotCallOnRetryWhenNoRetriesArePerformed() {
    final var onRetryCalled = new AtomicBoolean(false);
    final var policy = AsyncRetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .onRetry(ev -> onRetryCalled.set(true))
      .build();

    final var result = raiseResults(policy, Result.GOOD).join();

    assertThat(result).isEqualTo(Result.GOOD);
    assertThat(onRetryCalled.get()).isFalse();
  }

  @Test
  public void shouldCallOnRetryWithThePassedContext() {
    final var context = new AtomicReference<Context>();
    final var policy = AsyncRetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .onRetry(ev -> context.set(ev.context()))
      .build();

    final var result =
      raiseResults(policy, Map.of("key1", "value1", "key2", "value2"), Result.FAULT, Result.GOOD).join();

    assertThat(result).isEqualTo(Result.GOOD);
    assertThat(context.get()).containsOnly(entry("key1", "value1"), entry("key2", "value2"));
  }

  @Test
  public void shouldCallOnRetryWithThePassedContextWhenExecuteAndCapture() {
    final var context = new AtomicReference<Context>();
    final var policy = AsyncRetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .onRetry(ev -> context.set(ev.context()))
      .build();

    final var result = raiseResultsOnExecuteAndCapture(policy, Map.of("key1", "value1", "key2", "value2"),
      Result.FAULT, Result.GOOD).join();

    assertThat(result.isSuccess()).isTrue();
    final var success = (PolicyResult.Success<Result>) result;
    assertThat(success.getResult()).isEqualTo(Result.GOOD);
    assertThat(context.get()).containsOnly(entry("key1", "value1"), entry("key2", "value2"));
  }

  @Test
  public void shouldNotCallOnRetryWhenRetryCountIsZero() {
    final var retryInvoked = new AtomicBoolean(false);
    final var policy = AsyncRetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .maxRetryCount(0)
      .build();

    final var result = raiseResults(policy, Result.FAULT, Result.GOOD).join();

    assertThat(result).isEqualTo(Result.FAULT);
    assertThat(retryInvoked.get()).isFalse();
  }

  @Test
  public void contextShouldBeEmptyIfExecuteNotCalledWithAnyContextData() {
    final var actualContext = new AtomicReference<Context>();
    final var policy = AsyncRetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .onRetry(ev -> actualContext.set(ev.context()))
      .build();

    raiseResults(policy, Result.FAULT, Result.GOOD).join();

    assertThat(actualContext.get()).isEmpty();
  }

  @Test
  public void shouldCreateNewContextForEachCallToExecute() {
    final var actualContextValue = new AtomicReference<String>();
    final var policy = AsyncRetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .onRetry(ev -> actualContextValue.set(ev.context().get("key").toString()))
      .build();

    raiseResults(policy, Map.of("key", "original_value"), Result.FAULT, Result.GOOD).join();

    assertThat(actualContextValue.get()).isEqualTo("original_value");

    raiseResults(policy, Map.of("key", "new_value"), Result.FAULT, Result.GOOD).join();

    assertThat(actualContextValue.get()).isEqualTo("new_value");
  }

  @Test
  public void shouldCreateNewContextForEachCallToExecuteAndCapture() {
    final var actualContextValue = new AtomicReference<String>();
    final var policy = AsyncRetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .onRetry(ev -> actualContextValue.set(ev.context().get("key").toString()))
      .build();

    raiseResultsOnExecuteAndCapture(policy, Map.of("key", "original_value"), Result.FAULT, Result.GOOD).join();

    assertThat(actualContextValue.get()).isEqualTo("original_value");

    raiseResults(policy, Map.of("key", "new_value"), Result.FAULT, Result.GOOD).join();

    assertThat(actualContextValue.get()).isEqualTo("new_value");
  }

  @Test
  public void shouldCreateNewStateForEachCallToPolicy() {
    final var policy = AsyncRetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .maxRetryCount(1)
      .build();

    final var result1 = raiseResults(policy, Result.FAULT, Result.GOOD).join();
    final var result2 = raiseResults(policy, Result.FAULT, Result.GOOD).join();

    assertThat(result1).isEqualTo(Result.GOOD);
    assertThat(result2).isEqualTo(Result.GOOD);
  }

  @Test
  public void shouldExecuteAllTriesWhenFaultingAndNotCancelled() {
    final var policy = AsyncRetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .build();
    final var attemptsInvoked = new AtomicInteger(0);

    final var result = raiseResults(policy, (Runnable) attemptsInvoked::incrementAndGet, Result.FAULT, Result.FAULT,
      Result.FAULT, Result.GOOD).join();

    assertThat(result).isEqualTo(Result.GOOD);
    assertThat(attemptsInvoked.get()).isEqualTo(1 + 3);
  }

  @Test
  public void shouldNotExecuteActionWhenCancelledBeforeExecute() {
    final var policy = AsyncRetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .build();
    final Executor delayedExecutor = CompletableFuture.delayedExecutor(200, TimeUnit.MILLISECONDS);
    final CompletableFuture<Void> cancellationFuture = CompletableFuture.runAsync(() -> {
    }, delayedExecutor);
    final var attemptsInvoked = new AtomicInteger(0);
    final Supplier<CompletableFuture<Void>> action = () -> cancellationFuture.thenRun(attemptsInvoked::incrementAndGet);
    cancellationFuture.cancel(false);

    final var throwable = catchThrowable(() -> raiseResults(policy, action, Result.FAULT, Result.FAULT, Result.FAULT,
      Result.GOOD).join());

    assertThat(throwable).isInstanceOf(CompletionException.class)
      .hasCauseInstanceOf(CancellationException.class);
    assertThat(attemptsInvoked.get()).isEqualTo(0);
  }

  @Test
  public void shouldReportCancellationDuringOtherwiseNonFaultingActionExecutionAndCancelFurtherRetries() {
    final var policy = AsyncRetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .build();
    final var attemptsInvoked = new AtomicInteger(0);

    final var throwable = catchThrowable(() -> raiseResultsAndOrCancellation(policy, 1,
      attemptsInvoked::incrementAndGet, Result.GOOD, Result.GOOD, Result.GOOD, Result.GOOD).join());

    assertThat(throwable).isInstanceOf(CancellationException.class);
    assertThat(attemptsInvoked.get()).isEqualTo(1);
  }

  @Test
  public void shouldReportCancellationDuringFaultingInitialActionExecutionAndCancelFurtherRetries() {
    final var policy = AsyncRetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .build();
    final var attemptsInvoked = new AtomicInteger(0);

    final var throwable = catchThrowable(() -> raiseResultsAndOrCancellation(policy, 1,
      attemptsInvoked::incrementAndGet, Result.FAULT, Result.FAULT, Result.FAULT, Result.GOOD).join());

    assertThat(throwable).isInstanceOf(CancellationException.class);
    assertThat(attemptsInvoked.get()).isEqualTo(1);
  }

  @Test
  public void shouldReportCancellationDuringFaultingRetriedActionExecutionAndCancelFurtherRetries() {
    final var policy = AsyncRetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .build();
    final var attemptsInvoked = new AtomicInteger(0);

    final var throwable = catchThrowable(() -> raiseResultsAndOrCancellation(policy, 2,
      attemptsInvoked::incrementAndGet, Result.FAULT, Result.FAULT, Result.FAULT, Result.GOOD).join());

    assertThat(throwable).isInstanceOf(CancellationException.class);
    assertThat(attemptsInvoked.get()).isEqualTo(2);
  }

  @Test
  public void shouldReportCancellationDuringFaultingLastRetryExecution() {
    final var policy = AsyncRetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .build();
    final var attemptsInvoked = new AtomicInteger(0);

    final var throwable = catchThrowable(() -> raiseResultsAndOrCancellation(policy, 1 + 3,
      attemptsInvoked::incrementAndGet, Result.FAULT, Result.FAULT, Result.FAULT, Result.FAULT, Result.GOOD).join());

    assertThat(throwable).isInstanceOf(CancellationException.class);
    assertThat(attemptsInvoked.get()).isEqualTo(1 + 3);
  }
}
