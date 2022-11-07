package htnl5.yarl.retry;

import htnl5.yarl.helpers.Result;
import htnl5.yarl.helpers.ResultClass;
import org.junit.jupiter.api.Test;

import static htnl5.yarl.helpers.PolicyUtils.raiseResults;
import static htnl5.yarl.helpers.PolicyUtils.raiseResultsAndOrExceptions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class RetryPolicyMixedResultExceptionTest {
  @Test
  public void shouldHandleExceptionWhenHandlingExceptionsOnly() throws Throwable {
    final var policy = RetryPolicy.<Result>builder()
      .handle(ArithmeticException.class)
      .maxRetryCount(1)
      .build();

    final var result = raiseResultsAndOrExceptions(policy, Result.class, new ArithmeticException(), Result.GOOD);

    assertThat(result).isEqualTo(Result.GOOD);
  }

  @Test
  public void shouldThrowUnhandledExceptionWhenHandlingExceptionsOnly() {
    final var policy = RetryPolicy.<Result>builder()
      .handle(ArithmeticException.class)
      .maxRetryCount(1)
      .build();

    final var throwable = catchThrowable(() -> raiseResultsAndOrExceptions(policy, Result.class,
      new IllegalArgumentException(), Result.GOOD));

    assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void shouldHandleBothExceptionAndSpecifiedResultIfRaisedSameNumberOfTimeAsRetryCount() throws Throwable {
    final var policy = RetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .handle(ArithmeticException.class)
      .maxRetryCount(2)
      .build();

    final var result = raiseResultsAndOrExceptions(policy, Result.class, Result.FAULT, new ArithmeticException(),
      Result.GOOD);

    assertThat(result).isEqualTo(Result.GOOD);
  }

  @Test
  public void shouldHandleBothExceptionsAndSpecifiedResultsIfRaisedSameNumberOfTimesAsRetryCount() throws Throwable {
    final var policy = RetryPolicy.<Result>builder()
      .handle(ArithmeticException.class)
      .handleResult(Result.FAULT)
      .handle(IllegalArgumentException.class)
      .handleResult(Result.FAULT_AGAIN)
      .maxRetryCount(4)
      .build();

    final var result = raiseResultsAndOrExceptions(policy, Result.class, Result.FAULT, new ArithmeticException(),
      new IllegalArgumentException(), Result.FAULT_AGAIN, Result.GOOD);

    assertThat(result).isEqualTo(Result.GOOD);
  }

  @Test
  public void shouldReturnHandledResultWhenHandledResultReturnedNextAfterRetriesExhaustHandlingBothExceptionsAndSpecifiedResults() throws Throwable {
    final var policy = RetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .handle(ArithmeticException.class)
      .handleResult(Result.FAULT_AGAIN)
      .handle(IllegalArgumentException.class)
      .maxRetryCount(3)
      .build();

    final var result = raiseResultsAndOrExceptions(policy, Result.class, Result.FAULT, new ArithmeticException(),
      new IllegalArgumentException(), Result.FAULT_AGAIN, Result.GOOD);

    assertThat(result).isEqualTo(Result.FAULT_AGAIN);
  }

  @Test
  public void shouldThrowWhenExceptionThrownNextAfterRetriesExhaustHandlingBothExceptionsAndSpecifiedResults() {
    final var policy = RetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .handle(ArithmeticException.class)
      .handleResult(Result.FAULT_AGAIN)
      .handle(IllegalArgumentException.class)
      .maxRetryCount(3)
      .build();

    final var throwable = catchThrowable(() -> raiseResultsAndOrExceptions(policy, Result.class, Result.FAULT,
      new ArithmeticException(), Result.FAULT_AGAIN, new IllegalArgumentException(), Result.GOOD));

    assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void shouldReturnUnhandledResultIfNotOneOfResultsOrExceptionsSpecified() throws Throwable {
    final var policy = RetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .handle(ArithmeticException.class)
      .maxRetryCount(2)
      .build();

    final var result = raiseResults(policy, Result.FAULT_AGAIN);

    assertThat(result).isEqualTo(Result.FAULT_AGAIN);
  }

  @Test
  public void shouldThrowIfNotOneOfResultsOrExceptionsHandled() {
    final var policy = RetryPolicy.<Result>builder()
      .handle(ArithmeticException.class)
      .handleResult(Result.FAULT)
      .maxRetryCount(2)
      .build();

    final var throwable = catchThrowable(() -> raiseResultsAndOrExceptions(policy, Result.class,
      new IllegalArgumentException(), Result.GOOD));

    assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void shouldHandleBothExceptionsAndSpecifiedResultsWithPredicates() throws Throwable {
    final var policy = RetryPolicy.<ResultClass>builder()
      .handle(IllegalArgumentException.class, e -> e.getMessage().equals("key"))
      .handleResult(r -> r.getResultCode() == Result.FAULT)
      .maxRetryCount(2)
      .build();

    final var result = raiseResultsAndOrExceptions(policy, ResultClass.class, new ResultClass(Result.FAULT),
      new IllegalArgumentException("key"), new ResultClass(Result.GOOD));

    assertThat(result.getResultCode()).isEqualTo(Result.GOOD);
  }

  @Test
  public void shouldThrowIfExceptionPredicateNotMatched() {
    final var policy = RetryPolicy.<ResultClass>builder()
      .handle(IllegalArgumentException.class, e -> e.getMessage().equals("key"))
      .handleResult(r -> r.getResultCode() == Result.FAULT)
      .maxRetryCount(2)
      .build();

    final var throwable = catchThrowable(() -> raiseResultsAndOrExceptions(policy, ResultClass.class,
      new ResultClass(Result.FAULT), new IllegalArgumentException("value"), new ResultClass(Result.GOOD)));

    assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void shouldReturnUnhandledResultIfResultPredicateNotMatched() throws Throwable {
    final var policy = RetryPolicy.<ResultClass>builder()
      .handle(IllegalArgumentException.class, e -> e.getMessage().equals("key"))
      .handleResult(r -> r.getResultCode() == Result.FAULT)
      .maxRetryCount(2)
      .build();

    final var result = raiseResultsAndOrExceptions(policy, ResultClass.class, new IllegalArgumentException("key"),
      new ResultClass(Result.FAULT_AGAIN), new ResultClass(Result.GOOD));

    assertThat(result.getResultCode()).isEqualTo(Result.FAULT_AGAIN);
  }
}
