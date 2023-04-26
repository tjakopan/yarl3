package htnl5.yarl.retry;

import htnl5.yarl.helpers.Result;
import htnl5.yarl.helpers.ResultClass;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionException;

import static htnl5.yarl.helpers.AsyncPolicyUtils.raiseResults;
import static htnl5.yarl.helpers.AsyncPolicyUtils.raiseResultsAndOrExceptions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class AsyncRetryPolicyMixedResultExceptionTest {
  @Test
  public void shouldHandleExceptionWhenHandlingExceptionsOnly() {
    final var policy = AsyncRetryPolicy.<Result>builder()
      .handle(ArithmeticException.class)
      .maxRetryCount(1)
      .build();

    final var result = raiseResultsAndOrExceptions(policy, Result.class, new ArithmeticException(), Result.GOOD).join();

    assertThat(result).isEqualTo(Result.GOOD);
  }

  @Test
  public void shouldThrowUnhandledExceptionWhenHandlingExceptionsOnly() {
    final var policy = AsyncRetryPolicy.<Result>builder()
      .handle(ArithmeticException.class)
      .maxRetryCount(1)
      .build();

    final var throwable = catchThrowable(() -> raiseResultsAndOrExceptions(policy, Result.class,
      new IllegalArgumentException(), Result.GOOD).join());

    assertThat(throwable).isInstanceOf(CompletionException.class)
      .hasCauseInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void shouldHandleBothExceptionAndSpecifiedResultIfRaisedSameNumberOfTimeAsRetryCount() {
    final var policy = AsyncRetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .handle(ArithmeticException.class)
      .maxRetryCount(2)
      .build();

    final var result = raiseResultsAndOrExceptions(policy, Result.class, Result.FAULT, new ArithmeticException(),
      Result.GOOD)
      .join();

    assertThat(result).isEqualTo(Result.GOOD);
  }

  @Test
  public void shouldHandleBothExceptionsAndSpecifiedResultsIfRaisedSameNumberOfTimesAsRetryCount() {
    final var policy = AsyncRetryPolicy.<Result>builder()
      .handle(ArithmeticException.class)
      .handleResult(Result.FAULT)
      .handle(IllegalArgumentException.class)
      .handleResult(Result.FAULT_AGAIN)
      .maxRetryCount(4)
      .build();

    final var result = raiseResultsAndOrExceptions(policy, Result.class, Result.FAULT, new ArithmeticException(),
      new IllegalArgumentException(), Result.FAULT_AGAIN, Result.GOOD)
      .join();

    assertThat(result).isEqualTo(Result.GOOD);
  }

  @Test
  public void shouldReturnHandledResultWhenHandledResultReturnedNextAfterRetriesExhaustHandlingBothExceptionsAndSpecifiedResults() {
    final var policy = AsyncRetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .handle(ArithmeticException.class)
      .handleResult(Result.FAULT_AGAIN)
      .handle(IllegalArgumentException.class)
      .maxRetryCount(3)
      .build();

    final var result = raiseResultsAndOrExceptions(policy, Result.class, Result.FAULT, new ArithmeticException(),
      new IllegalArgumentException(), Result.FAULT_AGAIN, Result.GOOD)
      .join();

    assertThat(result).isEqualTo(Result.FAULT_AGAIN);
  }

  @Test
  public void shouldThrowWhenExceptionThrownNextAfterRetriesExhaustHandlingBothExceptionsAndSpecifiedResults() {
    final var policy = AsyncRetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .handle(ArithmeticException.class)
      .handleResult(Result.FAULT_AGAIN)
      .handle(IllegalArgumentException.class)
      .maxRetryCount(3)
      .build();

    final var throwable = catchThrowable(() -> raiseResultsAndOrExceptions(policy, Result.class, Result.FAULT,
      new ArithmeticException(), Result.FAULT_AGAIN, new IllegalArgumentException(), Result.GOOD)
      .join());

    assertThat(throwable).isInstanceOf(CompletionException.class)
      .hasCauseInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void shouldReturnUnhandledResultIfNotOneOfResultsOrExceptionsSpecified() {
    final var policy = AsyncRetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .handle(ArithmeticException.class)
      .maxRetryCount(2)
      .build();

    final var result = raiseResults(policy, Result.FAULT_AGAIN).join();

    assertThat(result).isEqualTo(Result.FAULT_AGAIN);
  }

  @Test
  public void shouldThrowIfNotOneOfResultsOrExceptionsHandled() {
    final var policy = AsyncRetryPolicy.<Result>builder()
      .handle(ArithmeticException.class)
      .handleResult(Result.FAULT)
      .maxRetryCount(2)
      .build();

    final var throwable = catchThrowable(() -> raiseResultsAndOrExceptions(policy, Result.class,
      new IllegalArgumentException(), Result.GOOD)
      .join());

    assertThat(throwable).isInstanceOf(CompletionException.class)
      .hasCauseInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void shouldHandleBothExceptionsAndSpecifiedResultsWithPredicates() {
    final var policy = AsyncRetryPolicy.<ResultClass>builder()
      .handle(IllegalArgumentException.class, e -> e.getMessage().equals("key"))
      .handleResult(r -> r.getResultCode() == Result.FAULT)
      .maxRetryCount(2)
      .build();

    final var result = raiseResultsAndOrExceptions(policy, ResultClass.class, new ResultClass(Result.FAULT),
      new IllegalArgumentException("key"), new ResultClass(Result.GOOD))
      .join();

    assertThat(result.getResultCode()).isEqualTo(Result.GOOD);
  }

  @Test
  public void shouldThrowIfExceptionPredicateNotMatched() {
    final var policy = AsyncRetryPolicy.<ResultClass>builder()
      .handle(IllegalArgumentException.class, e -> e.getMessage().equals("key"))
      .handleResult(r -> r.getResultCode() == Result.FAULT)
      .maxRetryCount(2)
      .build();

    final var throwable = catchThrowable(() -> raiseResultsAndOrExceptions(policy, ResultClass.class,
      new ResultClass(Result.FAULT), new IllegalArgumentException("value"), new ResultClass(Result.GOOD))
      .join());

    assertThat(throwable).isInstanceOf(CompletionException.class)
      .hasCauseInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void shouldReturnUnhandledResultIfResultPredicateNotMatched() {
    final var policy = AsyncRetryPolicy.<ResultClass>builder()
      .handle(IllegalArgumentException.class, e -> e.getMessage().equals("key"))
      .handleResult(r -> r.getResultCode() == Result.FAULT)
      .maxRetryCount(2)
      .build();

    final var result = raiseResultsAndOrExceptions(policy, ResultClass.class, new IllegalArgumentException("key"),
      new ResultClass(Result.FAULT_AGAIN), new ResultClass(Result.GOOD))
      .join();

    assertThat(result.getResultCode()).isEqualTo(Result.FAULT_AGAIN);
  }
}
