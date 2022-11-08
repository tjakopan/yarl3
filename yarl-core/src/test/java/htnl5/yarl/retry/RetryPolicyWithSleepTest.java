package htnl5.yarl.retry;

import htnl5.yarl.helpers.Result;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static htnl5.yarl.helpers.PolicyUtils.raiseExceptions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class RetryPolicyWithSleepTest {
  private static final TestSleeper NO_OP_SLEEPER = new TestSleeper(d -> {
  });

  @Test
  public void shouldBeAbleToCalculateSleepDurationsBasedOnTheHandledFault() throws Throwable {
    final var expectedSleepDurations = Map.of(
      Result.FAULT, Duration.ofSeconds(2),
      Result.FAULT_AGAIN, Duration.ofSeconds(4)
    );
    final var actualSleepDurations = new ArrayList<Duration>();
    final var policy = RetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .handleResult(Result.FAULT_AGAIN)
      .maxRetryCount(2)
      .sleepDurationProvider(event -> event.outcome().fold(expectedSleepDurations::get, e -> Duration.ZERO))
      .onRetry(event -> actualSleepDurations.add(event.sleepDuration()))
      .sleeper(NO_OP_SLEEPER)
      .build();

    final var iter = expectedSleepDurations.keySet().iterator();
    policy.execute(() -> {
      if (iter.hasNext()) return iter.next();
      else return Result.UNDEFINED;
    });

    assertThat(actualSleepDurations).containsExactlyElementsOf(expectedSleepDurations.values());
  }

  @Test
  public void shouldThrowWhenSleepDurationsIsNull() {
    final var throwable = catchThrowable(() ->
      RetryPolicy.<Void>builder()
        .handle(ArithmeticException.class)
        .sleepDurations((Iterable<Duration>) null)
        .build());

    assertThat(throwable).isInstanceOf(NullPointerException.class)
      .hasMessageContaining("sleepDurations");
  }

  @Test
  public void shouldNotThrowWhenSpecifiedExceptionThrownSameNumberOfTimesAsThereAreSleepDurations() throws Throwable {
    final var policy = RetryPolicy.<Void>builder()
      .handle(ArithmeticException.class)
      .sleepDurations(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3))
      .sleeper(NO_OP_SLEEPER)
      .build();

    raiseExceptions(policy, 3, i -> new ArithmeticException());
  }

  @Test
  public void shouldNotThrowWhenOneOfTheSpecifiedExceptionsThrownSameNumberOfTimesAsThereAreSleepDurations()
    throws Throwable {
    final var policy = RetryPolicy.<Void>builder()
      .handle(ArithmeticException.class)
      .handle(IllegalArgumentException.class)
      .sleepDurations(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3))
      .sleeper(NO_OP_SLEEPER)
      .build();

    raiseExceptions(policy, 3, i -> new IllegalArgumentException());
  }

  @Test
  public void shouldNotThrowWhenSpecifiedExceptionThrownLessNumberOfTimesThanThereAreSleepDurations() throws Throwable {
    final var policy = RetryPolicy.<Void>builder()
      .handle(ArithmeticException.class)
      .sleepDurations(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3))
      .sleeper(NO_OP_SLEEPER)
      .build();

    raiseExceptions(policy, 2, i -> new ArithmeticException());
  }

  @Test
  public void shouldNotThrowWhenOnOfTheSpecifiedExceptionsThrownLessNumberOfTimesThanThereAreSleepDurations()
    throws Throwable {
    final var policy = RetryPolicy.<Void>builder()
      .handle(ArithmeticException.class)
      .handle(IllegalArgumentException.class)
      .sleepDurations(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3))
      .sleeper(NO_OP_SLEEPER)
      .build();

    raiseExceptions(policy, 2, i -> new ArithmeticException());
  }

  @Test
  public void shouldThrowWhenSpecifiedExceptionThrownMoreTimesThanThereAreSleepDurations() {
    final var policy = RetryPolicy.<Void>builder()
      .handle(ArithmeticException.class)
      .sleepDurations(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3))
      .sleeper(NO_OP_SLEEPER)
      .build();

    final var throwable = catchThrowable(() -> raiseExceptions(policy, 3 + 1,
      i -> new ArithmeticException()));

    assertThat(throwable).isInstanceOf(ArithmeticException.class);
  }

  @Test
  public void shouldThrowWhenOneOfTheSpecifiedExceptionsThrownMoreTimesThanThereAreSleepDurations() {
    final var policy = RetryPolicy.<Void>builder()
      .handle(ArithmeticException.class)
      .handle(IllegalArgumentException.class)
      .sleepDurations(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3))
      .sleeper(NO_OP_SLEEPER)
      .build();

    final var throwable = catchThrowable(() -> raiseExceptions(policy, 3 + 1,
      i -> new IllegalArgumentException()));

    assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void shouldThrowWhenExceptionThrownIsNotTheSpecifiedExceptionType() {
    final var policy = RetryPolicy.<Void>builder()
      .handle(ArithmeticException.class)
      .sleepDurations(List.of())
      .sleeper(NO_OP_SLEEPER)
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
      .sleepDurations(List.of())
      .sleeper(NO_OP_SLEEPER)
      .build();

    final var throwable = catchThrowable(() -> raiseExceptions(policy, 1,
      i -> new NullPointerException()));

    assertThat(throwable).isInstanceOf(NullPointerException.class);
  }

  @Test
  public void shouldThrowWhenSpecifiedExceptionPredicateIsNotSatisfied() {
    final var policy = RetryPolicy.<Void>builder()
      .handle(ArithmeticException.class, e -> false)
      .sleepDurations(List.of())
      .sleeper(NO_OP_SLEEPER)
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
      .sleepDurations(List.of())
      .sleeper(NO_OP_SLEEPER)
      .build();

    final var throwable = catchThrowable(() -> raiseExceptions(policy, 1,
      i -> new IllegalArgumentException()));

    assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void shouldNotThrowWhenSpecifiedExceptionPredicateIsSatisfied() throws Throwable {
    final var policy = RetryPolicy.<Void>builder()
      .handle(ArithmeticException.class, e -> true)
      .sleepDurations(Duration.ofSeconds(1))
      .sleeper(NO_OP_SLEEPER)
      .build();

    raiseExceptions(policy, 1, i -> new ArithmeticException());
  }

  @Test
  public void shouldNotThrowWhenOneOfTheSpecifiedExceptionPredicatesIsSatisfied() throws Throwable {
    final var policy = RetryPolicy.<Void>builder()
      .handle(ArithmeticException.class, e -> true)
      .handle(IllegalArgumentException.class, e -> true)
      .sleepDurations(Duration.ofSeconds(1))
      .sleeper(NO_OP_SLEEPER)
      .build();

    raiseExceptions(policy, 1, i -> new IllegalArgumentException());
  }

  @Test
  public void shouldSleepForTheSpecifiedDurationEachRetryWhenSpecifiedExceptionThrownSameNumberOfTimesAsThereAreSleepDurations()
    throws Throwable {
    final var totalTimeSlept = new AtomicReference<>(Duration.ZERO);
    final var sleeper = new TestSleeper(d -> totalTimeSlept.accumulateAndGet(d, Duration::plus));
    final var policy = RetryPolicy.<Void>builder()
      .handle(ArithmeticException.class)
      .sleepDurations(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3))
      .sleeper(sleeper)
      .build();

    raiseExceptions(policy, 3, i -> new ArithmeticException());

    assertThat(totalTimeSlept.get()).isEqualTo(Duration.ofSeconds(1 + 2 + 3));
  }

  @Test
  public void shouldSleepForTheSpecifiedDurationEachRetryWhenSpecifiedExceptionThrownMoreNumberOfTimesThanThereAreSleepDurations() {
    final var totalTimeSlept = new AtomicReference<>(Duration.ZERO);
    final var sleeper = new TestSleeper(d -> totalTimeSlept.accumulateAndGet(d, Duration::plus));
    final var policy = RetryPolicy.<Void>builder()
      .handle(ArithmeticException.class)
      .sleepDurations(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3))
      .sleeper(sleeper)
      .build();

    final var throwable = catchThrowable(() -> raiseExceptions(policy, 3 + 1,
      i -> new ArithmeticException()));

    assertThat(throwable).isInstanceOf(ArithmeticException.class);
    assertThat(totalTimeSlept.get()).isEqualTo(Duration.ofSeconds(1 + 2 + 3));
  }

  @Test
  public void shouldSleepForTheSpecifiedDurationEachRetryWhenSpecifiedExceptionThrownLessNumberOfTimesThanThereAreSleepDurations()
    throws Throwable {
    final var totalTimeSlept = new AtomicReference<>(Duration.ZERO);
    final var sleeper = new TestSleeper(d -> totalTimeSlept.accumulateAndGet(d, Duration::plus));
    final var policy = RetryPolicy.<Void>builder()
      .handle(ArithmeticException.class)
      .sleepDurations(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3))
      .sleeper(sleeper)
      .build();

    raiseExceptions(policy, 2, i -> new ArithmeticException());

    assertThat(totalTimeSlept.get()).isEqualTo(Duration.ofSeconds(1 + 2));
  }

  @Test
  public void shouldNotSleepIfNoRetries() {
    final var totalTimeSlept = new AtomicReference<>(Duration.ZERO);
    final var sleeper = new TestSleeper(d -> totalTimeSlept.accumulateAndGet(d, Duration::plus));
    final var policy = RetryPolicy.<Void>builder()
      .handle(ArithmeticException.class)
      .sleepDurations(List.of())
      .sleeper(sleeper)
      .build();

    final var throwable = catchThrowable(() -> raiseExceptions(policy, 1,
      i -> new NullPointerException()));

    assertThat(throwable).isInstanceOf(NullPointerException.class);
    assertThat(totalTimeSlept.get()).isEqualTo(Duration.ZERO);
  }

  @Test
  public void shouldCallOnRetryOnEachRetryWithTheCurrentDurations() throws Throwable {
    final var expectedSleepDurations =
      List.of(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3));
    final var actualSleepDurations = new ArrayList<Duration>();
    final var policy = RetryPolicy.<Void>builder()
      .handle(ArithmeticException.class)
      .sleepDurations(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3))
      .onRetry(event -> actualSleepDurations.add(event.sleepDuration()))
      .sleeper(NO_OP_SLEEPER)
      .build();

    raiseExceptions(policy, 3, i -> new ArithmeticException());

    assertThat(actualSleepDurations).containsExactlyElementsOf(expectedSleepDurations);
  }

  @Test
  public void shouldThrowWhenSleepDurationProviderIsNull() {
    final var throwable = catchThrowable(() ->
      RetryPolicy.<Void>builder()
        .handle(ArithmeticException.class)
        .maxRetryCount(1)
        .sleepDurationProvider(null)
        .build());

    assertThat(throwable).isInstanceOf(NullPointerException.class)
      .hasMessageContaining("sleepDurationProvider");
  }

  @Test
  public void shouldCalculateSleepDurationFromCurrentRetryAttemptAndDurationProvider() throws Throwable {
    final var expectedSleepDurations = List.of(Duration.ofSeconds(2), Duration.ofSeconds(4),
      Duration.ofSeconds(8), Duration.ofSeconds(16), Duration.ofSeconds(32));
    final var actualSleepDurations = new ArrayList<Duration>();
    final var policy = RetryPolicy.<Void>builder()
      .handle(ArithmeticException.class)
      .maxRetryCount(5)
      .sleepDurationProvider(event -> Duration.ofSeconds((long) Math.pow(2, event.tryCount())))
      .onRetry(event -> actualSleepDurations.add(event.sleepDuration()))
      .sleeper(NO_OP_SLEEPER)
      .build();

    raiseExceptions(policy, 5, i -> new ArithmeticException());

    assertThat(actualSleepDurations).containsExactlyElementsOf(expectedSleepDurations);
  }

  @Test
  public void shouldBeAbleToPassHandledExceptionToSleepDurationProvider() throws Throwable {
    final var capturedException = new AtomicReference<Throwable>();
    final var exception = new ArithmeticException();
    final var policy = RetryPolicy.<Void>builder()
      .handle(ArithmeticException.class)
      .maxRetryCount(5)
      .sleepDurationProvider(event -> {
        event.outcome().onFailure(capturedException::set);
        return Duration.ZERO;
      })
      .sleeper(NO_OP_SLEEPER)
      .build();

    raiseExceptions(policy, 1, i -> exception);

    assertThat(capturedException.get()).isSameAs(exception);
  }

  @Test
  public void shouldBeAbleToCalculateSleepDurationBasedOnTheHandledException() throws Throwable {
    final var expectedSleepDurations = Map.of(
      new ArithmeticException(), Duration.ofSeconds(2),
      new IllegalArgumentException(), Duration.ofSeconds(2)
    );
    final var actualSleepDurations = new ArrayList<Duration>();
    //noinspection SuspiciousMethodCalls
    final var policy = RetryPolicy.<Void>builder()
      .handle(Exception.class)
      .maxRetryCount(2)
      .sleepDurationProvider(event -> event.outcome().fold(v -> Duration.ZERO, expectedSleepDurations::get))
      .onRetry(event -> actualSleepDurations.add(event.sleepDuration()))
      .sleeper(NO_OP_SLEEPER)
      .build();

    final var iter = expectedSleepDurations.keySet().iterator();
    policy.execute(() -> {
      if (iter.hasNext()) throw iter.next();
      return null;
    });

    assertThat(actualSleepDurations).containsExactlyElementsOf(expectedSleepDurations.values());
  }

  @Test
  public void shouldBeAbleToPassSleepDurationFromExecutionToSleepDurationProviderViaContext() throws Throwable {
    final var expectedSleepDuration = Duration.ofSeconds(1);
    final var actualSleepDuration = new AtomicReference<Duration>();
    final var defaultSleepDuration = Duration.ofSeconds(30);
    final var policy = RetryPolicy.<Void>builder()
      .handle(ArithmeticException.class)
      .maxRetryCount(1)
      .sleepDurationProvider(event -> (Duration) event.context().getOrDefault("RetryAfter", defaultSleepDuration))
      .onRetry(event -> actualSleepDuration.set(event.sleepDuration()))
      .sleeper(NO_OP_SLEEPER)
      .build();
    final Map<String, Object> contextData = new HashMap<>();
    contextData.put("RetryAfter", defaultSleepDuration);

    final var failedOnce = new AtomicBoolean(false);
    policy.execute(contextData, ctx -> {
      ctx.put("RetryAfter", expectedSleepDuration);
      if (!failedOnce.get()) {
        failedOnce.set(true);
        throw new ArithmeticException();
      }
      return null;
    });

    assertThat(actualSleepDuration.get()).isEqualTo(expectedSleepDuration);
  }
}
