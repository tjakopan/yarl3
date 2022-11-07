package htnl5.yarl.retry;

import htnl5.yarl.Context;
import htnl5.yarl.PolicyResult;
import htnl5.yarl.helpers.Result;
import htnl5.yarl.helpers.ResultClass;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static htnl5.yarl.helpers.PolicyUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class RetryPolicyTest {
  @Test
  public void shouldThrowWhenRetryCountIsLessThanZero() {
    final var throwable = catchThrowable(() ->
      RetryPolicy.<Result>builder()
        .handleResult(Result.FAULT)
        .maxRetryCount(-1)
        .build());

    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Retry count");
  }

  @Test
  public void shouldThrowWhenOnRetryActionIsNull() {
    final var throwable = catchThrowable(() ->
      RetryPolicy.<Result>builder()
        .handleResult(Result.FAULT)
        .onRetry(null)
        .build());

    assertThat(throwable).isInstanceOf(NullPointerException.class)
      .hasMessageContaining("onRetry");
  }

  @Test
  public void shouldCreateNewStateForEachCallToPolicy() throws Throwable {
    final var policy = RetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .maxRetryCount(1)
      .build();

    final var result1 = raiseResults(policy, Result.FAULT, Result.GOOD);
    final var result2 = raiseResults(policy, Result.FAULT, Result.GOOD);

    assertThat(result1).isEqualTo(Result.GOOD);
    assertThat(result2).isEqualTo(Result.GOOD);
  }

  //<editor-fold desc="result handling tests">
  @Test
  public void shouldNotReturnHandledResultWhenHandledResultRaisedSameNumberOfTimesAsRetryCount() throws Throwable {
    final var policy = RetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .maxRetryCount(3)
      .build();

    final var result = raiseResults(policy, Result.FAULT, Result.FAULT, Result.FAULT, Result.GOOD);

    assertThat(result).isEqualTo(Result.GOOD);
  }

  @Test
  public void shouldNotReturnHandledResultWhenOneOfTheHandledResultsRaisedSameNumberOfTimesAsRetryCount()
    throws Throwable {
    final var policy = RetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .handleResult(Result.FAULT_AGAIN)
      .maxRetryCount(3)
      .build();

    final var result = raiseResults(policy, Result.FAULT_AGAIN, Result.FAULT_AGAIN, Result.FAULT_AGAIN,
      Result.GOOD);

    assertThat(result).isEqualTo(Result.GOOD);
  }

  @Test
  public void shouldNotReturnHandledResultWhenHandledResultRaisedLessNumberOfTimesThanRetryCount() throws Throwable {
    final var policy = RetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .maxRetryCount(3)
      .build();

    final var result = raiseResults(policy, Result.FAULT, Result.GOOD);

    assertThat(result).isEqualTo(Result.GOOD);
  }

  @Test
  public void shouldNotReturnHandledResultWhenAllOfTheHandledResultsRaisedLessNumberOfTimesThanRetryCount() throws Throwable {
    final var policy = RetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .handleResult(Result.FAULT_AGAIN)
      .maxRetryCount(3)
      .build();

    final var result = raiseResults(policy, Result.FAULT, Result.FAULT_AGAIN, Result.GOOD);

    assertThat(result).isEqualTo(Result.GOOD);
  }

  @Test
  public void shouldReturnHandledResultWhenHandledResultRaisedMoreTimesThanRetryCount() throws Throwable {
    final var policy = RetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .maxRetryCount(3)
      .build();

    final var result = raiseResults(policy, Result.FAULT, Result.FAULT, Result.FAULT, Result.FAULT, Result.GOOD);

    assertThat(result).isEqualTo(Result.FAULT);
  }

  @Test
  public void shouldReturnHandledResultWhenOneOfTheHandledResultsIsRaisedMoreTimesThanRetryCount() throws Throwable {
    final var policy = RetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .handleResult(Result.FAULT_AGAIN)
      .maxRetryCount(3)
      .build();

    final var result = raiseResults(policy, Result.FAULT_AGAIN, Result.FAULT_AGAIN, Result.FAULT_AGAIN,
      Result.FAULT_AGAIN, Result.GOOD);

    assertThat(result).isEqualTo(Result.FAULT_AGAIN);
  }

  @Test
  public void shouldReturnResultWhenResultIsNotTheSpecifiedHandledResult() throws Throwable {
    final var policy = RetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .maxRetryCount(1)
      .build();

    final var result = raiseResults(policy, Result.FAULT_AGAIN, Result.GOOD);

    assertThat(result).isEqualTo(Result.FAULT_AGAIN);
  }

  @Test
  public void shouldReturnResultWhenResultIsNotOneOfTheSpecifiedHandledResults() throws Throwable {
    final var policy = RetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .handleResult(Result.FAULT_AGAIN)
      .maxRetryCount(1)
      .build();

    final var result = raiseResults(policy, Result.FAULT_YET_AGAIN, Result.GOOD);

    assertThat(result).isEqualTo(Result.FAULT_YET_AGAIN);
  }

  @Test
  public void shouldReturnResultWhenSpecifiedResultPredicateIsNotSatisfied() throws Throwable {
    final var policy = RetryPolicy.<ResultClass>builder()
      .handleResult(r -> r.getResultCode() == Result.FAULT)
      .maxRetryCount(1)
      .build();

    final var result = raiseResults(policy, new ResultClass(Result.FAULT_AGAIN), new ResultClass(Result.GOOD));

    assertThat(result.getResultCode()).isEqualTo(Result.FAULT_AGAIN);
  }

  @Test
  public void shouldReturnResultWhenNoneOfTheSpecifiedResultPredicatesAreSatisfied() throws Throwable {
    final var policy = RetryPolicy.<ResultClass>builder()
      .handleResult(r -> r.getResultCode() == Result.FAULT)
      .handleResult(r -> r.getResultCode() == Result.FAULT_AGAIN)
      .maxRetryCount(1)
      .build();

    final var result = raiseResults(policy, new ResultClass(Result.FAULT_YET_AGAIN),
      new ResultClass(Result.GOOD));

    assertThat(result.getResultCode()).isEqualTo(Result.FAULT_YET_AGAIN);
  }

  @Test
  public void shouldNotReturnHandledResultWhenSpecifiedResultPredicateIsSatisfied() throws Throwable {
    final var policy = RetryPolicy.<ResultClass>builder()
      .handleResult(r -> r.getResultCode() == Result.FAULT)
      .maxRetryCount(1)
      .build();

    final var result = raiseResults(policy, new ResultClass(Result.FAULT), new ResultClass(Result.GOOD));

    assertThat(result.getResultCode()).isEqualTo(Result.GOOD);
  }

  @Test
  public void shouldNotReturnHandledResultWhenOneOfTheSpecifiedResultPredicatesIsSatisfied() throws Throwable {
    final var policy = RetryPolicy.<ResultClass>builder()
      .handleResult(r -> r.getResultCode() == Result.FAULT)
      .handleResult(r -> r.getResultCode() == Result.FAULT_AGAIN)
      .maxRetryCount(1)
      .build();

    final var result = raiseResults(policy, new ResultClass(Result.FAULT_AGAIN), new ResultClass(Result.GOOD));

    assertThat(result.getResultCode()).isEqualTo(Result.GOOD);
  }
  //</editor-fold>

  //<editor-fold desc="exception handling tests">
  @Test
  public void shouldNotThrowWhenSpecifiedExceptionThrownSameNumberOfTimesAsRetryCount() throws Throwable {
    final var policy = RetryPolicy.<Void>builder()
      .handle(ArithmeticException.class)
      .maxRetryCount(3)
      .build();

    raiseExceptions(policy, 3, i -> new ArithmeticException());
  }

  @Test
  public void shouldNotThrowWhenOneOfTheSpecifiedExceptionsThrownSameNumberOfTimesAsRetryCount() throws Throwable {
    final var policy = RetryPolicy.<Void>builder()
      .handle(ArithmeticException.class)
      .handle(IllegalArgumentException.class)
      .maxRetryCount(3)
      .build();

    raiseExceptions(policy, 3, i -> new IllegalArgumentException());
  }

  @Test
  public void shouldNotThrowWhenSpecifiedExceptionThrownLessNumberOfTimesThanRetryCount() throws Throwable {
    final var policy = RetryPolicy.<Void>builder()
      .handle(ArithmeticException.class)
      .maxRetryCount(3)
      .build();

    raiseExceptions(policy, 1, i -> new ArithmeticException());
  }

  @Test
  public void shouldNotThrowWhenOneOfTheSpecifiedExceptionsThrownLessNumberOfTimesThanRetryCount() throws Throwable {
    final var policy = RetryPolicy.<Void>builder()
      .handle(ArithmeticException.class)
      .handle(IllegalArgumentException.class)
      .maxRetryCount(3)
      .build();

    raiseExceptions(policy, 1, i -> new IllegalArgumentException());
  }

  @Test
  public void shouldThrowWhenSpecifiedExceptionThrownMoreTimesThanRetryCount() {
    final var policy = RetryPolicy.<Void>builder()
      .handle(ArithmeticException.class)
      .maxRetryCount(3)
      .build();

    final var throwable = catchThrowable(() -> raiseExceptions(policy, 3 + 1,
      i -> new ArithmeticException()));


    assertThat(throwable).isInstanceOf(ArithmeticException.class);
  }

  @Test
  public void shouldThrowWhenOneOfTheSpecifiedExceptionsAreThrownMoreTimesThanRetryCount() {
    final var policy = RetryPolicy.<Void>builder()
      .handle(ArithmeticException.class)
      .handle(IllegalArgumentException.class)
      .maxRetryCount(3)
      .build();

    final var throwable = catchThrowable(() -> raiseExceptions(policy, 3 + 1,
      i -> new IllegalArgumentException()));

    assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void shouldThrowWhenExceptionThrownIsNotTheSpecifiedExceptionType() {
    final var policy = RetryPolicy.<Void>builder()
      .handle(ArithmeticException.class)
      .maxRetryCount(1)
      .build();

    final var throwable = catchThrowable(() -> raiseExceptions(policy, 1,
      i -> new NullPointerException()));

    assertThat(throwable).isInstanceOf(NullPointerException.class);
  }

  @Test
  public void shouldThrowWhenExceptionThrownIsNotOneOfTheSpecifiedExceptionTypes() {
    final var policy = RetryPolicy.<Void>builder()
      .handle(ArithmeticException.class)
      .handle(IllegalArgumentException.class)
      .maxRetryCount(1)
      .build();

    final var throwable = catchThrowable(() -> raiseExceptions(policy, 1,
      i -> new NullPointerException()));

    assertThat(throwable).isInstanceOf(NullPointerException.class);
  }

  @Test
  public void shouldThrowWhenSpecifiedExceptionPredicateIsNotSatisfied() {
    final var policy = RetryPolicy.<Void>builder()
      .handle(ArithmeticException.class, e -> false)
      .maxRetryCount(1)
      .build();

    final var throwable = catchThrowable(() -> raiseExceptions(policy, 1,
      i -> new ArithmeticException()));

    assertThat(throwable).isInstanceOf(ArithmeticException.class);
  }

  @Test
  public void shouldThrowWhenNoneOfTheSpecifiedExceptionPredicatesAreSatisfied() {
    final var policy = RetryPolicy.<Void>builder()
      .handle(ArithmeticException.class, e -> false)
      .handle(IllegalArgumentException.class, e -> false)
      .maxRetryCount(1)
      .build();

    final var throwable = catchThrowable(() -> raiseExceptions(policy, 1,
      i -> new IllegalArgumentException()));

    assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void shouldNotThrowWhenSpecifiedExceptionPredicateIsSatisfied() throws Throwable {
    final var policy = RetryPolicy.<Void>builder()
      .handle(ArithmeticException.class, e -> true)
      .maxRetryCount(1)
      .build();

    raiseExceptions(policy, 1, i -> new ArithmeticException());
  }

  @Test
  public void shouldNotThrowWhenOneOfTheSpecifiedExceptionPredicatesAreSatisfied() throws Throwable {
    final var policy = RetryPolicy.<Void>builder()
      .handle(ArithmeticException.class, e -> true)
      .handle(IllegalArgumentException.class, e -> true)
      .maxRetryCount(1)
      .build();

    raiseExceptions(policy, 1, i -> new IllegalArgumentException());
  }
  //</editor-fold>

  //<editor-fold desc="onRetry tests">
  @Test
  public void shouldCallOnRetryOnEachRetryWithTheCurrentRetryCount() throws Throwable {
    final var expectedRetryCounts = List.of(1, 2, 3);
    final var actualRetryCounts = new ArrayList<Integer>();
    final var policy = RetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .maxRetryCount(3)
      .onRetry(event -> actualRetryCounts.add(event.tryCount()))
      .build();

    final var result = raiseResults(policy, Result.FAULT, Result.FAULT, Result.FAULT, Result.GOOD);

    assertThat(result).isEqualTo(Result.GOOD);
    assertThat(actualRetryCounts).containsExactlyElementsOf(expectedRetryCounts);
  }

  @Test
  public void shouldCallOnRetryOnEachRetryWithTheCurrentHandledResult() throws Throwable {
    final var expectedFaults = List.of("List #1", "List #2", "List #3");
    final var actualFaults = new ArrayList<String>();
    final var policy = RetryPolicy.<ResultClass>builder()
      .handleResult(r -> r.getResultCode() == Result.FAULT)
      .maxRetryCount(3)
      .onRetry(event -> event.outcome().onSuccess(r -> actualFaults.add(r.getSomeString())))
      .build();
    final var resultsToRaise = expectedFaults.stream()
      .map(s -> new ResultClass(Result.FAULT, s))
      .collect(Collectors.toList());
    resultsToRaise.add(new ResultClass(Result.FAULT));

    final var result = raiseResults(policy, resultsToRaise.toArray(ResultClass[]::new));

    assertThat(result.getResultCode()).isEqualTo(Result.FAULT);
    assertThat(actualFaults).containsExactlyElementsOf(expectedFaults);
  }

  @Test
  public void shouldCallOnRetryOnEachRetryWithTheCurrentException() throws Throwable {
    final var expectedMessages = List.of("Exception #1", "Exception #2", "Exception #3");
    final var actualExceptions = new ArrayList<Throwable>();
    final var policy = RetryPolicy.<Void>builder()
      .handle(ArithmeticException.class)
      .maxRetryCount(3)
      .onRetry(event -> event.outcome().onFailure(actualExceptions::add))
      .build();

    raiseExceptions(policy, 3, i -> new ArithmeticException("Exception #" + i));

    final var actualMessages = actualExceptions.stream()
      .map(Throwable::getMessage)
      .collect(Collectors.toList());
    assertThat(actualMessages).containsExactlyElementsOf(expectedMessages);
  }

  @Test
  public void shouldCallOnRetryWithAHandledCause() throws Throwable {
    final var actualCause = new AtomicReference<Throwable>();
    final var policy = RetryPolicy.<Void>builder()
      .handleCause(ArithmeticException.class)
      .maxRetryCount(3)
      .onRetry(event -> event.outcome().onFailure(actualCause::set))
      .build();
    final var expectedCause = new ArithmeticException();
    final var exception = new Exception(expectedCause);

    raiseExceptions(policy, 1, i -> exception);

    assertThat(actualCause.get()).isSameAs(expectedCause);
  }

  @Test
  public void shouldCallOnRetryWithANestedHandledCause() throws Throwable {
    final var actualCause = new AtomicReference<Throwable>();
    final var policy = RetryPolicy.<Void>builder()
      .handleCause(ArithmeticException.class)
      .maxRetryCount(3)
      .onRetry(event -> event.outcome().onFailure(actualCause::set))
      .build();
    final var expectedCause = new ArithmeticException();
    final var exception = new Exception(new Exception(expectedCause));

    raiseExceptions(policy, 1, i -> exception);

    assertThat(actualCause.get()).isSameAs(expectedCause);
  }

  @Test
  public void shouldNotCallOnRetryWhenNoRetriesArePerformed() throws Throwable {
    final var onRetryCalled = new AtomicBoolean(false);
    final var policy = RetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .maxRetryCount(1)
      .onRetry(event -> onRetryCalled.set(true))
      .build();

    final var result = raiseResults(policy, Result.GOOD);

    assertThat(result).isEqualTo(Result.GOOD);
    assertThat(onRetryCalled.get()).isFalse();
  }

  @Test
  public void shouldCallOnRetryWithThePassedContext() throws Throwable {
    final var actualContext = new AtomicReference<Context>();
    final var policy = RetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .maxRetryCount(1)
      .onRetry(event -> actualContext.set(event.context()))
      .build();
    final Map<String, Object> contextData = Map.of("key1", "value1", "key2", "value2");

    final var result = raiseResults(policy, contextData, Result.FAULT, Result.GOOD);

    assertThat(result).isEqualTo(Result.GOOD);
    assertThat(actualContext.get()).containsKeys("key1", "key2");
    assertThat(actualContext.get()).containsValues("value1", "value2");
  }

  @Test
  public void shouldCallOnRetryWithThePassedContextWhenExecuteAndCapture() {
    final var actualContext = new AtomicReference<Context>();
    final var policy = RetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .maxRetryCount(1)
      .onRetry(event -> actualContext.set(event.context()))
      .build();
    final Map<String, Object> contextData = Map.of("key1", "value1", "key2", "value2");

    final var result = raiseResultsOnExecuteAndCapture(policy, contextData, Result.FAULT, Result.GOOD);

    assertThat(result.isSuccess()).isTrue();
    final var success = (PolicyResult.Success<Result>) result;
    assertThat(success.getResult()).isEqualTo(Result.GOOD);
    assertThat(actualContext.get()).containsKeys("key1", "key2");
    assertThat(actualContext.get()).containsValues("value1", "value2");
  }

  @Test
  public void shouldNotCallOnRetryWhenRetryCountIsZero() throws Throwable {
    final var onRetryCalled = new AtomicBoolean(false);
    final var policy = RetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .maxRetryCount(0)
      .onRetry(event -> onRetryCalled.set(true))
      .build();

    final var result = raiseResults(policy, Result.FAULT, Result.GOOD);

    assertThat(result).isEqualTo(Result.FAULT);
    assertThat(onRetryCalled.get()).isFalse();
  }
  //</editor-fold>

  //<editor-fold desc="context tests">
  @Test
  public void contextShouldBeEmptyIfExecuteNotCalledWithAnyContextData() throws Throwable {
    final var actualContext = new AtomicReference<Context>();
    final var policy = RetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .maxRetryCount(1)
      .onRetry(event -> actualContext.set(event.context()))
      .build();

    raiseResults(policy, Result.FAULT, Result.GOOD);

    assertThat(actualContext.get()).isEmpty();
  }

  @Test
  public void shouldCreateNewContextForEachCallToExecute() throws Throwable {
    final var actualContextValue = new AtomicReference<String>();
    final var policy = RetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .maxRetryCount(1)
      .onRetry(event -> actualContextValue.set(event.context().get("key").toString()))
      .build();
    Map<String, Object> contextData = Map.of("key", "original_value");

    raiseResults(policy, contextData, Result.FAULT, Result.GOOD);

    assertThat(actualContextValue.get()).isEqualTo("original_value");

    contextData = Map.of("key", "new_value");
    raiseResults(policy, contextData, Result.FAULT, Result.GOOD);

    assertThat(actualContextValue.get()).isEqualTo("new_value");
  }

  @Test
  public void shouldCreateNewContextForEachCallToExecuteAndCapture() {
    final var actualContextValue = new AtomicReference<String>();
    final var policy = RetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .maxRetryCount(1)
      .onRetry(event -> actualContextValue.set(event.context().get("key").toString()))
      .build();
    Map<String, Object> contextData = Map.of("key", "original_value");

    raiseResultsOnExecuteAndCapture(policy, contextData, Result.FAULT, Result.GOOD);

    assertThat(actualContextValue.get()).isEqualTo("original_value");

    contextData = Map.of("key", "new_value");
    raiseResultsOnExecuteAndCapture(policy, contextData, Result.FAULT, Result.GOOD);

    assertThat(actualContextValue.get()).isEqualTo("new_value");
  }
  //</editor-fold>
}
