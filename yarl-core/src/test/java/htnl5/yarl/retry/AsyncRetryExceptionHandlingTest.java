package htnl5.yarl.retry;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionException;

import static htnl5.yarl.helpers.AsyncPolicyUtils.raiseException;
import static htnl5.yarl.helpers.AsyncPolicyUtils.raiseExceptions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class AsyncRetryExceptionHandlingTest {
  @Test
  public void shouldNotThrowWhenSpecifiedExceptionThrownSameNumberOfTimesAsRetryCount() {
    final var policy = AsyncRetryPolicy.builder()
      .handle(ArithmeticException.class)
      .maxRetryCount(3)
      .build();

    assertDoesNotThrow(() -> raiseExceptions(policy, 3, ArithmeticException.class).join());
  }

  @Test
  public void shouldNotThrowWhenOneOfTheSpecifiedExceptionsThrownSameNumberOfTimesAsRetryCount() {
    final var policy = AsyncRetryPolicy.builder()
      .handle(ArithmeticException.class)
      .handle(IllegalArgumentException.class)
      .maxRetryCount(3)
      .build();

    assertDoesNotThrow(() -> raiseExceptions(policy, 3, IllegalArgumentException.class).join());
  }

  @Test
  public void shouldNotThrowWhenSpecifiedExceptionThrownLessNumberOfTimesThanRetryCount() {
    final var policy = AsyncRetryPolicy.builder()
      .handle(ArithmeticException.class)
      .maxRetryCount(3)
      .build();

    assertDoesNotThrow(() -> raiseException(policy, ArithmeticException.class).join());
  }

  @Test
  public void shouldNotThrowWhenOneOfTheSpecifiedExceptionsThrownLessNumberOfTimesThanRetryCount() {
    final var policy = AsyncRetryPolicy.builder()
      .handle(ArithmeticException.class)
      .handle(IllegalArgumentException.class)
      .maxRetryCount(3)
      .build();

    assertDoesNotThrow(() -> raiseException(policy, IllegalArgumentException.class).join());
  }

  @Test
  public void shouldThrowWhenSpecifiedExceptionThrownMoreTimesThanRetryCount() {
    final var policy = AsyncRetryPolicy.builder()
      .handle(ArithmeticException.class)
      .maxRetryCount(3)
      .build();

    final var throwable = catchThrowable(() ->
      raiseExceptions(policy, 3 + 1, ArithmeticException.class).join());

    assertThat(throwable).isInstanceOf(CompletionException.class)
      .hasCauseInstanceOf(ArithmeticException.class);
  }

  @Test
  public void shouldThrowWhenOneOfTheSpecifiedExceptionsThrownMoreTimesThanRetryCount() {
    final var policy = AsyncRetryPolicy.builder()
      .handle(ArithmeticException.class)
      .handle(IllegalArgumentException.class)
      .maxRetryCount(3)
      .build();

    final var throwable = catchThrowable(() ->
      raiseExceptions(policy, 3 + 1, IllegalArgumentException.class).join());

    assertThat(throwable).isInstanceOf(CompletionException.class)
      .hasCauseInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void shouldThrowWhenExceptionThrownIsNotTheSpecifiedExceptionType() {
    final var policy = AsyncRetryPolicy.builder()
      .handle(ArithmeticException.class)
      .build();

    final var throwable = catchThrowable(() -> raiseException(policy, NullPointerException.class).join());

    assertThat(throwable).isInstanceOf(CompletionException.class)
      .hasCauseInstanceOf(NullPointerException.class);
  }

  @Test
  public void shouldThrowWhenExceptionThrownIsNotOneOfTheSpecifiedExceptionTypes() {
    final var policy = AsyncRetryPolicy.builder()
      .handle(ArithmeticException.class)
      .handle(IllegalArgumentException.class)
      .build();

    final var throwable = catchThrowable(() -> raiseException(policy, NullPointerException.class).join());

    assertThat(throwable).isInstanceOf(CompletionException.class)
      .hasCauseInstanceOf(NullPointerException.class);
  }

  @Test
  public void shouldThrowWhenSpecifiedExceptionPredicateIsNotSatisfied() {
    final var policy = AsyncRetryPolicy.builder()
      .handle(ArithmeticException.class, e -> false)
      .build();

    final var throwable = catchThrowable(() -> raiseException(policy, ArithmeticException.class).join());

    assertThat(throwable).isInstanceOf(CompletionException.class)
      .hasCauseInstanceOf(ArithmeticException.class);
  }

  @Test
  public void shouldThrowWhenNoneOfTheSpecifiedExceptionPredicatesAreSatisfied() {
    final var policy = AsyncRetryPolicy.builder()
      .handle(ArithmeticException.class, e -> false)
      .handle(IllegalArgumentException.class, e -> false)
      .build();

    final var throwable = catchThrowable(() -> raiseException(policy, IllegalArgumentException.class).join());

    assertThat(throwable).isInstanceOf(CompletionException.class)
      .hasCauseInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void shouldNotThrowWhenSpecifiedExceptionPredicateIsSatisfied() {
    final var policy = AsyncRetryPolicy.builder()
      .handle(ArithmeticException.class, e -> true)
      .build();

    assertDoesNotThrow(() -> raiseException(policy, ArithmeticException.class).join());
  }

  @Test
  public void shouldNotThrowWhenOneOfTheSpecifiedExceptionPredicatesIsSatisfied() {
    final var policy = AsyncRetryPolicy.builder()
      .handle(ArithmeticException.class, e -> true)
      .handle(IllegalArgumentException.class, e -> true)
      .build();

    assertDoesNotThrow(() -> raiseException(policy, IllegalArgumentException.class).join());
  }
}
