package htnl5.yarl.circuitbreaker;

import htnl5.yarl.DelegateResult;
import htnl5.yarl.helpers.MutableClock;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static htnl5.yarl.helpers.PolicyUtils.raiseException;
import static htnl5.yarl.helpers.PolicyUtils.raiseExceptions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class AdvancedCircuitBreakerTest {
  //<editor-fold desc="configuration tests">
  @Test
  public void shouldBeAbleToHandleADurationOfMaxValue() {
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, Duration.ofSeconds(10), 4, Duration.ofMillis(Long.MAX_VALUE))
      .handle(ArithmeticException.class)
      .build();

    final var throwable = catchThrowable(() ->
      raiseExceptions(breaker, 1, i -> new ArithmeticException()));

    assertThat(throwable).isInstanceOf(ArithmeticException.class);
  }

  @Test
  public void shouldThrowIfFailureThresholdIsZero() {
    final var throwable = catchThrowable(() -> CircuitBreakerPolicy
      .<Void>advancedBuilder(0, Duration.ofSeconds(10), 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .build());

    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("failureThreshold");
  }

  @Test
  public void shouldThrowIfFailureThresholdIsLessThanZero() {
    final var throwable = catchThrowable(() -> CircuitBreakerPolicy
      .<Void>advancedBuilder(-0.5, Duration.ofSeconds(10), 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .build());

    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("failureThreshold");
  }

  @Test
  public void shouldBeAbleToHandleAFailureThresholdOfOne() {
    CircuitBreakerPolicy
      .<Void>advancedBuilder(1.0, Duration.ofSeconds(10), 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .build();
  }

  @Test
  public void shouldThrowIfFailureThresholdIsGreaterThanOne() {
    final var throwable = catchThrowable(() -> CircuitBreakerPolicy
      .<Void>advancedBuilder(1.01, Duration.ofSeconds(10), 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .build());

    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("failureThreshold");
  }

  @Test
  public void shouldThrowIfSamplingDurationIsLessThanResolutionOfCircuit() {
    final var throwable = catchThrowable(() -> CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, Duration.ofMillis(19), 4, Duration.ofSeconds(30))
      .build());

    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("samplingDuration");
  }

  @Test
  public void shouldNotThrowIfSamplingDurationIsResolutionOfCircuit() {
    CircuitBreakerPolicy
      .advancedBuilder(0.5, Duration.ofMillis(20), 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .build();
  }

  @Test
  public void shouldThrowIfMinimumThroughputIsOne() {
    final var throwable = catchThrowable(() -> CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, Duration.ofSeconds(10), 1, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .build());

    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("minimumThroughput");
  }

  @Test
  public void shouldThrowIfMinimumThroughputIsLessThanOne() {
    final var throwable = catchThrowable(() -> CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, Duration.ofSeconds(10), 0, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .build());

    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("minimumThroughput");
  }

  @Test
  public void shouldThrowIfDurationOfBreakIsLessThanZero() {
    final var throwable = catchThrowable(() -> CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, Duration.ofSeconds(10), 4, Duration.ofSeconds(-1))
      .handle(ArithmeticException.class)
      .build());

    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("durationOfBreak");
  }

  @Test
  public void shouldBeAbleToHandleDurationOfBreakZero() {
    CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, Duration.ofSeconds(10), 4, Duration.ZERO)
      .handle(ArithmeticException.class)
      .build();
  }

  @Test
  public void shouldInitialiseToClosedState() {
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, Duration.ofSeconds(10), 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .build();

    assertThat(breaker.getState()).isEqualTo(CircuitBreakerState.CLOSED);
  }
  //</editor-fold>

  //<editor-fold desc="failure threshold tests">

  //<editor-fold desc="tests that are independent of health metrics implementation">

  // Tests on the AdvancedCircuitBreaker operations typically use a breaker:
  // - with a failure threshold od >= 50%,
  // - and a throughput threshold of 4
  // - across a 10 seconds period.
  // These provide easy values for failure and throughput thresholds each being met and not-met, in combination.

  @Test
  public void shouldNotOpenCircuitIfFailureThresholdAndMinimumThroughputIsEqualisedButLastCallIsSuccess()
    throws Throwable {
    final var clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, Duration.ofSeconds(10), 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .clock(clock)
      .build();

    // Three of three actions in this test throw handled failures.
    final var throwable1 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state1 = breaker.getState();
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    // Failure threshold exceeded, but throughput not yet.
    // Throughput threshold will be exceeded by the below successful call, but we never break on successful call,
    // hence don't break on this.
    breaker.execute(() -> null);
    final var state4 = breaker.getState();
    // No adjustment to clock, so all exceptions were raised within same sampling duration.

    assertThat(throwable1).isInstanceOf(ArithmeticException.class);
    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(state4).isEqualTo(CircuitBreakerState.CLOSED);
  }

  @Test
  public void shouldNotOpenCircuitIfExceptionsRaisedAreNotOneOfTheSpecifiedExceptions() {
    final var clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, Duration.ofSeconds(10), 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .handle(IllegalArgumentException.class)
      .clock(clock)
      .build();

    // Four of four actions in this test throw unhandled failures.
    final var throwable1 = catchThrowable(() -> raiseException(breaker, NullPointerException::new));
    final var state1 = breaker.getState();
    final var throwable2 = catchThrowable(() -> raiseException(breaker, NullPointerException::new));
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, NullPointerException::new));
    final var state3 = breaker.getState();
    final var throwable4 = catchThrowable(() -> raiseException(breaker, NullPointerException::new));
    final var state4 = breaker.getState();

    assertThat(throwable1).isInstanceOf(NullPointerException.class);
    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable2).isInstanceOf(NullPointerException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(NullPointerException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(NullPointerException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.CLOSED);
  }
  //</editor-fold>

  //<editor-fold desc="with sampling duration higher than 199 ms so that multiple windows are used">

  // Tests on the AdvancedCircuitBreaker operations typically use a breaker:
  // - with a failure threshold od >= 50%,
  // - and a throughput threshold of 4
  // - across a 10 seconds period.
  // These provide easy values for failure and throughput thresholds each being met and not-met, in combination.

  @Test
  public void shouldOpenCircuitBlockingExecutionsAndNotingTheLastRaisedExceptionIfFailureThresholdExceededAndThroughputThresholdEqualledWithingSamplingDurationInSameWindow() {
    final var clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, Duration.ofSeconds(10), 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .clock(clock)
      .build();

    // Four of four actions in this test throw handled failures.
    final var throwable1 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state1 = breaker.getState();
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    final var throwable4 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state4 = breaker.getState();
    // No adjustment to clock, so all exceptions were raised within same sampling duration.
    final var actionExecutedWhenBroken = new AtomicBoolean(false);
    final var throwable5 = catchThrowable(() -> breaker.execute(() -> {
      actionExecutedWhenBroken.set(true);
      return null;
    }));
    final var state5 = breaker.getState();

    assertThat(throwable1).isInstanceOf(ArithmeticException.class);
    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(throwable5).isInstanceOf(BrokenCircuitException.class)
      .hasMessage("The circuit is now open and is not allowing calls.")
      .hasCauseInstanceOf(ArithmeticException.class);
    assertThat(state5).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(actionExecutedWhenBroken.get()).isFalse();
  }

  @Test
  public void shouldOpenCircuitWithTheLastRaisedExceptionIfFailureThresholdExceededAndThroughputThresholdEqualledWithinSamplingDurationInDifferentWindows() {
    final var instant = Instant.now();
    final var clock = new MutableClock(instant, ZoneId.systemDefault());
    final var samplingDuration = Duration.ofSeconds(10);
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, samplingDuration, 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .clock(clock)
      .build();

    // Four of four actions in this test throw handled failures.
    final var throwable1 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state1 = breaker.getState();
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    // Placing the rest of invocations ('samplingDuration' / 2) + 1 seconds later ensures that even if there are only
    // two windows, then the invocations are placed in the second. They are still placed within same sampling duration.
    clock.setInstant(instant.plus(samplingDuration.dividedBy(2L)).plusSeconds(1L));
    final var throwable4 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state4 = breaker.getState();
    final var throwable5 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state5 = breaker.getState();

    assertThat(throwable1).isInstanceOf(ArithmeticException.class);
    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(throwable5).isInstanceOf(BrokenCircuitException.class)
      .hasMessage("The circuit is now open and is not allowing calls.")
      .hasCauseInstanceOf(ArithmeticException.class);
    assertThat(state5).isEqualTo(CircuitBreakerState.OPEN);
  }

  @Test
  public void shouldOpenCircuitWithTheLastRaisedExceptionIfFailureThresholdExceededThoughNotAllAreFailuresAndThroughputThresholdEqualledWithinSamplingDurationInSameWindow()
    throws Throwable {
    final var clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, Duration.ofSeconds(10), 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .clock(clock)
      .build();

    // Three of four actions in this test throw handled failures.
    breaker.execute(() -> null);
    final var state1 = breaker.getState();
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    final var throwable4 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state4 = breaker.getState();
    // No adjustment to clock, so all exceptions were raised within same sampling duration.
    final var throwable5 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state5 = breaker.getState();

    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(throwable5).isInstanceOf(BrokenCircuitException.class)
      .hasMessage("The circuit is now open and is not allowing calls.")
      .hasCauseInstanceOf(ArithmeticException.class);
    assertThat(state5).isEqualTo(CircuitBreakerState.OPEN);
  }

  @Test
  public void shouldOpenCircuitWithTheLastRaisedExceptionIfFailureThresholdExceededThoughNotAllAreFailuresAndThroughputThresholdEqualledWithinSamplingDurationInDifferentWindows()
    throws Throwable {
    final var instant = Instant.now();
    final var clock = new MutableClock(instant, ZoneId.systemDefault());
    final var samplingDuration = Duration.ofSeconds(10);
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, samplingDuration, 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .clock(clock)
      .build();

    // Three of four actions in this test throw handled failures.
    breaker.execute(() -> null);
    final var state1 = breaker.getState();
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    final var throwable4 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state4 = breaker.getState();
    // Placing the rest of invocations ('samplingDuration' / 2) + 1 seconds later ensures that even if there are only
    // two windows, then the invocations are placed in the second. They are still placed within same sampling duration.
    clock.setInstant(instant.plus(samplingDuration.dividedBy(2L)).plusSeconds(1L));
    final var throwable5 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state5 = breaker.getState();

    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(throwable5).isInstanceOf(BrokenCircuitException.class)
      .hasMessage("The circuit is now open and is not allowing calls.")
      .hasCauseInstanceOf(ArithmeticException.class);
    assertThat(state5).isEqualTo(CircuitBreakerState.OPEN);
  }

  @Test
  public void shouldOpenCircuitWithTheLastRaisedExceptionIfFailureThresholdEqualledAndThroughputThresholdEqualledWithinSamplingDurationInSameWindow()
    throws Throwable {
    final var clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, Duration.ofSeconds(10), 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .clock(clock)
      .build();

    // Two of four actions in this test throw handled failure.
    breaker.execute(() -> null);
    final var state1 = breaker.getState();
    breaker.execute(() -> null);
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    final var throwable4 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state4 = breaker.getState();
    // No adjustment to clock, so all exceptions were raised withing same sampling duration.
    final var throwable5 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state5 = breaker.getState();

    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(throwable5).isInstanceOf(BrokenCircuitException.class)
      .hasMessage("The circuit is now open and is not allowing calls.")
      .hasCauseInstanceOf(ArithmeticException.class);
    assertThat(state5).isEqualTo(CircuitBreakerState.OPEN);
  }

  @Test
  public void shouldOpenCircuitWithTheLastRaisedExceptionIfFailureThresholdEqualledAndThroughputThresholdEqualledWithinSamplingDurationInDifferentWindows()
    throws Throwable {
    final var instant = Instant.now();
    final var clock = new MutableClock(instant, ZoneId.systemDefault());
    final var samplingDuration = Duration.ofSeconds(10);
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, samplingDuration, 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .clock(clock)
      .build();

    // Two of four actions in this test throw handled failures.
    breaker.execute(() -> null);
    final var state1 = breaker.getState();
    breaker.execute(() -> null);
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    final var throwable4 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state4 = breaker.getState();
    // Placing the rest of invocations ('samplingDuration' / 2) + 1 seconds later ensures that even if there are only
    // two windows, then the invocations are placed in the second. They are still placed within same sampling duration.
    clock.setInstant(instant.plus(samplingDuration.dividedBy(2L)).plusSeconds(1L));
    final var throwable5 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state5 = breaker.getState();

    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(throwable5).isInstanceOf(BrokenCircuitException.class)
      .hasMessage("The circuit is now open and is not allowing calls.")
      .hasCauseInstanceOf(ArithmeticException.class);
    assertThat(state5).isEqualTo(CircuitBreakerState.OPEN);
  }

  @Test
  public void shouldNotOpenCircuitIfFailureThresholdExceededButThroughputThresholdNotMetBeforeSamplingDurationExpires() {
    final var instant = Instant.now();
    final var clock = new MutableClock(instant, ZoneId.systemDefault());
    final var samplingDuration = Duration.ofSeconds(10);
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, samplingDuration, 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .clock(clock)
      .build();

    // Four of four actions in this test throw handled failures, but only the first three within sampling duration.
    final var throwable1 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state1 = breaker.getState();
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    // Adjust clock so that sampling duration (clearly) expires. Fourth exception throw in the next recorded
    // sampling duration.
    clock.setInstant(instant.plus(samplingDuration.multipliedBy(2L)));
    final var throwable4 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state4 = breaker.getState();

    assertThat(throwable1).isInstanceOf(ArithmeticException.class);
    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.CLOSED);
  }

  @Test
  public void shouldNotOpenCircuitIfFailureThresholdExceededButThroughputThresholdNotMetBeforeSamplingDurationExpiresEveIfSamplingDurationExpireOnlyExactly() {
    final var instant = Instant.now();
    final var clock = new MutableClock(instant, ZoneId.systemDefault());
    final var samplingDuration = Duration.ofSeconds(10);
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, samplingDuration, 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .clock(clock)
      .build();

    // Four of four actions in this test throw handled failures, but only the first three within sampling duration.
    final var throwable1 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state1 = breaker.getState();
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    // Adjust clock so that sampling duration (just) expires. Fourth exception throw in following sampling duration.
    clock.setInstant(instant.plus(samplingDuration));
    final var throwable4 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state4 = breaker.getState();

    assertThat(throwable1).isInstanceOf(ArithmeticException.class);
    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.CLOSED);
  }

  @Test
  public void shouldNotOpenCircuitIfFailureThresholdExceededButThroughputThresholdNotMetBeforeSamplingDurationExpiresEvenIfErrorOccurringJustAtTheEndOfTheDuration() {
    final var instant = Instant.now();
    final var clock = new MutableClock(instant, ZoneId.systemDefault());
    final var samplingDuration = Duration.ofSeconds(10);
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, samplingDuration, 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .clock(clock)
      .build();

    // Four of four actions in this test throw handled failures, but only the first three within the original
    // sampling duration.
    // Two actions at the start of the original sampling duration.
    final var throwable1 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state1 = breaker.getState();
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state2 = breaker.getState();
    // Creates a new window right at the end of the original sampling duration.
    clock.setInstant(instant.plus(samplingDuration).minusMillis(1L));
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    // Adjust clock so that sampling duration (just) expires. Fourth exception throw in following sampling duration.
    clock.setInstant(instant.plus(samplingDuration));
    final var throwable4 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state4 = breaker.getState();

    assertThat(throwable1).isInstanceOf(ArithmeticException.class);
    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.CLOSED);
  }

  @Test
  public void shouldOpenCircuitWithTheLastRaisedExceptionIfFailureThresholdEqualledAndThroughputThresholdEqualledEvenIfOnlyJustWithinSamplingDuration() {
    final var instant = Instant.now();
    final var clock = new MutableClock(instant, ZoneId.systemDefault());
    final var samplingDuration = Duration.ofSeconds(10);
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, samplingDuration, 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .clock(clock)
      .build();

    // Four of four actions in this test throw handled failures.
    final var throwable1 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state1 = breaker.getState();
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    // Adjust clock so that sampling duration doesn't quite expire. Fourth exception throw in same sampling duration.
    clock.setInstant(instant.plus(samplingDuration).minusMillis(1L));
    final var throwable4 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state4 = breaker.getState();
    final var throwable5 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state5 = breaker.getState();

    assertThat(throwable1).isInstanceOf(ArithmeticException.class);
    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(throwable5).isInstanceOf(BrokenCircuitException.class)
      .hasMessage("The circuit is now open and is not allowing calls.")
      .hasCauseInstanceOf(ArithmeticException.class);
    assertThat(state5).isEqualTo(CircuitBreakerState.OPEN);
  }

  @Test
  public void shouldNotOpenCircuitIfFailureThresholdNotMetAndThroughputNotMet() throws Throwable {
    final var clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, Duration.ofSeconds(10), 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .clock(clock)
      .build();

    // One of three actions in this test throw handled failures.
    breaker.execute(() -> null);
    final var state1 = breaker.getState();
    breaker.execute(() -> null);
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    // No adjustment to clock, so all exception were raised within same sampling duration.

    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
  }

  @Test
  public void shouldNotOpenCircuitIfFailureThresholdNotMetButThroughputThresholdMetBeforeSamplingDurationExpires()
    throws Throwable {
    final var clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, Duration.ofSeconds(10), 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .clock(clock)
      .build();

    // One of four actions in this test throw handled failures.
    breaker.execute(() -> null);
    final var state1 = breaker.getState();
    breaker.execute(() -> null);
    final var state2 = breaker.getState();
    breaker.execute(() -> null);
    final var state3 = breaker.getState();
    final var throwable4 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state4 = breaker.getState();
    // No adjustment to clock, so all exception were raised within same sampling duration.

    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.CLOSED);
  }

  @Test
  public void shouldOpenCircuitIfFailuresAtTheEndOfLastSamplingDurationBelowFailureThresholdAndFailuresInBeginningOfNewSamplingDurationWhereTotalEqualsFailureThreshold()
    throws Throwable {
    final var instant = Instant.now();
    final var clock = new MutableClock(instant, ZoneId.systemDefault());
    final var samplingDuration = Duration.ofSeconds(10);
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, samplingDuration, 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .clock(clock)
      .build();

    // Executing a single invocation to ensure sampling duration is created. This invocation is not counted against
    // the threshold.
    breaker.execute(() -> null);
    final var state1 = breaker.getState();
    // The time is set to just at the end of the sampling duration ensuring the invocations are within the sampling
    // duration, but only barely.
    clock.setInstant(instant.plus(samplingDuration).minusMillis(1L));
    // Three of four actions in this test occur within the first sampling duration.
    breaker.execute(() -> null);
    final var state2 = breaker.getState();
    breaker.execute(() -> null);
    final var state3 = breaker.getState();
    final var throwable4 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state4 = breaker.getState();
    // Setting the time to just barely into the new sampling duration.
    clock.setInstant(instant.plus(samplingDuration));
    // This failure opens the circuit, because it is the second failure of four calls equalling the failure threshold.
    // The minimum threshold withing the defined sampling duration is met, when using rolling windows.
    final var throwable5 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state5 = breaker.getState();

    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable5).isInstanceOf(ArithmeticException.class);
    assertThat(state5).isEqualTo(CircuitBreakerState.OPEN);
  }

  @Test
  public void shouldNotOpenCircuitIfFailuresAtTheEndOfLastSamplingDurationAndFailuresInBeginningOfNewSamplingDurationWhenBelowMinimumThroughputThreshold()
    throws Throwable {
    final var instant = Instant.now();
    final var clock = new MutableClock(instant, ZoneId.systemDefault());
    final var samplingDuration = Duration.ofSeconds(10);
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, samplingDuration, 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .clock(clock)
      .build();

    // Executing a single invocation to ensure sampling duration is created. This invocation is not counted against
    // threshold.
    breaker.execute(() -> null);
    final var state1 = breaker.getState();
    // The time is set to just at the end of the sampling duration ensuring the invocations are within the sampling
    // duration, but only barely.
    clock.setInstant(instant.plus(samplingDuration).minusMillis(1L));
    // Two of three actions in this test occur within first sampling duration.
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    // Setting the time to just barely into the new sampling duration.
    clock.setInstant(instant.plus(samplingDuration));
    // A third failure occurs just at the beginning of the new sampling duration making the number of failure above
    // the failure threshold. However, the throughput is below the minimum threshold as to open the circuit.
    final var throwable4 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state4 = breaker.getState();

    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.CLOSED);
  }

  @Test
  public void shouldOpenCircuitIfFailuresInSecondWindowOfLastSamplingDurationAndFailuresInFirstWindowInNextSamplingDurationExceedsFailureThresholdAndMinimumThreshold()
    throws Throwable {
    final var instant = Instant.now();
    final var clock = new MutableClock(instant, ZoneId.systemDefault());
    final var samplingDuration = Duration.ofSeconds(10);
    final var numberOfWindowsDefinedInCircuitBreaker = 10;
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, samplingDuration, 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .clock(clock)
      .build();

    // Executing a single invocation to ensure sampling duration is created. This invocation is not counted against
    // threshold.
    breaker.execute(() -> null);
    final var state1 = breaker.getState();
    // Setting the time to the second window in the rolling metrics.
    clock.setInstant(instant.plus(samplingDuration.dividedBy(numberOfWindowsDefinedInCircuitBreaker)));
    // Three actions occur in the second window of the first sampling duration.
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    breaker.execute(() -> null);
    final var state4 = breaker.getState();
    // Setting the time to just barely into the new sampling duration.
    clock.setInstant(instant.plus(samplingDuration));
    final var throwable5 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state5 = breaker.getState();

    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(state4).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable5).isInstanceOf(ArithmeticException.class);
    assertThat(state5).isEqualTo(CircuitBreakerState.OPEN);
  }
  //</editor-fold>

  //<editor-fold desc="with sampling duration at 199 ms so that only single window is used">

  // Tests on the AdvancedCircuitBreaker operations typically use a breaker:
  // - with a failure threshold od >= 50%,
  // - and a throughput threshold of 4
  // - across a 199 ms period.
  // These provide easy values for failure and throughput thresholds each being met and not-met, in combination.

  @Test
  public void shouldOpenCircuitWithTheLastRaisedExceptionIfFailureThresholdExceededAndThroughputThresholdEqualledWithinSamplingDurationLowSamplingDuration() {
    final var clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, Duration.ofMillis(199), 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .clock(clock)
      .build();

    // Four of four actions in this test throw handled failures.
    final var throwable1 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state1 = breaker.getState();
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    // No adjustment to clock, so all exception raised within same sampling duration.
    final var throwable4 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state4 = breaker.getState();
    final var throwable5 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state5 = breaker.getState();

    assertThat(throwable1).isInstanceOf(ArithmeticException.class);
    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(throwable5).isInstanceOf(BrokenCircuitException.class)
      .hasMessage("The circuit is now open and is not allowing calls.")
      .hasCauseInstanceOf(ArithmeticException.class);
    assertThat(state5).isEqualTo(CircuitBreakerState.OPEN);
  }

  @Test
  public void shouldOpenCircuitWithTheLastRaisedExceptionIfFailureThresholdExceededThoughNotAllAreFailuresAndThroughputThresholdEqualledWithingSamplingDurationLowSamplingDuration()
    throws Throwable {
    final var clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, Duration.ofMillis(199), 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .clock(clock)
      .build();

    // Three of four actions in this test throw handled failures.
    breaker.execute(() -> null);
    final var state1 = breaker.getState();
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    final var throwable4 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state4 = breaker.getState();
    // No adjustment to clock, so all exception raised within same sampling duration.
    final var throwable5 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state5 = breaker.getState();

    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(throwable5).isInstanceOf(BrokenCircuitException.class)
      .hasMessage("The circuit is now open and is not allowing calls.")
      .hasCauseInstanceOf(ArithmeticException.class);
    assertThat(state5).isEqualTo(CircuitBreakerState.OPEN);
  }

  @Test
  public void shouldNotOpenCircuitIfFailureThresholdExceededButThroughputThresholdNotMetBeforeSamplingDurationExpiresLowSamplingDuration() {
    final var instant = Instant.now();
    final var clock = new MutableClock(instant, ZoneId.systemDefault());
    final var samplingDuration = Duration.ofMillis(199);
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, samplingDuration, 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .clock(clock)
      .build();

    // Four of four actions in this test throw failures, but only the first three within the sampling duration.
    final var throwable1 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state1 = breaker.getState();
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    // Adjust clock so that sampling duration (clearly) expires, fourth exception thrown in next recorded sampling
    // duration.
    clock.setInstant(instant.plus(samplingDuration.multipliedBy(2L)));
    final var throwable4 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state4 = breaker.getState();

    assertThat(throwable1).isInstanceOf(ArithmeticException.class);
    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.CLOSED);
  }

  @Test
  public void shouldNotOpenCircuitIfFailureThresholdExceededButThroughputThresholdNotMetBeforeSamplingDurationExpiresEvenIfSamplingDurationExpiresOnlyExactlyLowSamplingDuration()
    throws Throwable {
    final var instant = Instant.now();
    final var clock = new MutableClock(instant, ZoneId.systemDefault());
    final var samplingDuration = Duration.ofMillis(199);
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, samplingDuration, 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .clock(clock)
      .build();

    // Two of four actions in this test throw failures, but only the first three within the sampling duration.
    breaker.execute(() -> null);
    final var state1 = breaker.getState();
    breaker.execute(() -> null);
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    // Adjust clock so that sampling duration (just) expires, fourth exception thrown in following sampling
    // duration.
    clock.setInstant(instant.plus(samplingDuration));
    final var throwable4 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state4 = breaker.getState();

    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.CLOSED);
  }

  @Test
  public void shouldOpenCircuitWithTheLastRaisedExceptionIfFailureThresholdEqualledAndThroughputThresholdEqualledEvenIfOnlyJustWithinSamplingDurationLowSamplingDuration() {
    final var instant = Instant.now();
    final var clock = new MutableClock(instant, ZoneId.systemDefault());
    final var samplingDuration = Duration.ofMillis(199);
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, samplingDuration, 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .clock(clock)
      .build();

    // Four of four actions in this test throw failures.
    final var throwable1 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state1 = breaker.getState();
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    // Adjust clock so that sampling duration doesn't quite expire, fourth exception thrown in same sampling
    // duration.
    clock.setInstant(instant.plus(samplingDuration).minusMillis(1L));
    final var throwable4 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state4 = breaker.getState();
    final var throwable5 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state5 = breaker.getState();

    assertThat(throwable1).isInstanceOf(ArithmeticException.class);
    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(throwable5).isInstanceOf(BrokenCircuitException.class)
      .hasMessage("The circuit is now open and is not allowing calls.")
      .hasCauseInstanceOf(ArithmeticException.class);
    assertThat(state5).isEqualTo(CircuitBreakerState.OPEN);
  }

  @Test
  public void shouldNotOpenCircuitIfFailureThresholdNotMetAndThroughputThresholdNotMetLowSamplingDuration()
    throws Throwable {
    final var instant = Instant.now();
    final var clock = Clock.fixed(instant, ZoneId.systemDefault());
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, Duration.ofMillis(199), 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .clock(clock)
      .build();

    // One of three actions in this test throw handled failures.
    breaker.execute(() -> null);
    final var state1 = breaker.getState();
    breaker.execute(() -> null);
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    // No adjustment to clock, so all exceptions were raised within same sampling duration.

    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
  }

  @Test
  public void shouldNotOpenCircuitIfFailureThresholdNotMetButThroughputThresholdMetBeforeSamplingDurationExpiresLowSamplingDuration()
    throws Throwable {
    final var instant = Instant.now();
    final var clock = Clock.fixed(instant, ZoneId.systemDefault());
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, Duration.ofMillis(199), 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .clock(clock)
      .build();

    // One of three actions in this test throw handled failures.
    breaker.execute(() -> null);
    final var state1 = breaker.getState();
    breaker.execute(() -> null);
    final var state2 = breaker.getState();
    breaker.execute(() -> null);
    final var state3 = breaker.getState();
    final var throwable4 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state4 = breaker.getState();
    // No adjustment to clock, so all exceptions were raised within same sampling duration.

    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.CLOSED);
  }

  @Test
  public void shouldNotOpenCircuitIfFailuresAtTheEndOfLastSamplingDurationBelowFailureThresholdAndFailuresInBeginningOfNewSamplingDurationWhereTotalEqualsFailureThresholdLowSamplingDuration()
    throws Throwable {
    final var instant = Instant.now();
    final var clock = new MutableClock(instant, ZoneId.systemDefault());
    final var samplingDuration = Duration.ofMillis(199);
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, samplingDuration, 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .clock(clock)
      .build();

    // Executing a single invocation to ensure sampling duration is created. This invocation is not counted against
    // the threshold.
    breaker.execute(() -> null);
    final var state1 = breaker.getState();
    // The time is set to just at the end of the sampling duration ensuring the invocations are within the sampling
    // duration, but only barely.
    clock.setInstant(instant.plus(samplingDuration).minusMillis(1L));
    // Three of four actions in this test occur within the first sampling duration.
    breaker.execute(() -> null);
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    // Setting time to just barely into the new sampling duration.
    clock.setInstant(instant.plus(samplingDuration));
    // This failure does not open the circuit, because a new duration should have started and with such low sampling
    // duration, windows should not be used.
    final var throwable4 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state4 = breaker.getState();

    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.CLOSED);
  }
  //</editor-fold>
  //</editor-fold>

  //<editor-fold desc="open -> half-open -> open/closed tests">
  @Test
  public void shouldHalfOpenCircuitAfterTheSpecifiedDurationHasPassedWithFailuresInSameWindow() {
    final var instant = Instant.now();
    final var clock = new MutableClock(instant, ZoneId.systemDefault());
    final var durationOfBreak = Duration.ofSeconds(30);
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, Duration.ofSeconds(10), 4, durationOfBreak)
      .handle(ArithmeticException.class)
      .clock(clock)
      .build();

    // Four of four actions in this test throw handled failures.
    final var throwable1 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state1 = breaker.getState();
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    final var throwable4 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state4 = breaker.getState();
    clock.setInstant(instant.plus(durationOfBreak));
    // Duration has passed, circuit is now half open.
    final var state5 = breaker.getState();
    final var throwable5 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));

    assertThat(throwable1).isInstanceOf(ArithmeticException.class);
    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(throwable5).isInstanceOf(ArithmeticException.class);
    assertThat(state5).isEqualTo(CircuitBreakerState.HALF_OPEN);
  }

  @Test
  public void shouldHalfOpenCircuitAfterTheSpecifiedDurationHasPassedWithFailuresInDifferentWindows() {
    final var instant = Instant.now();
    final var clock = new MutableClock(instant, ZoneId.systemDefault());
    final var samplingDuration = Duration.ofSeconds(10);
    final var durationOfBreak = Duration.ofSeconds(30);
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, samplingDuration, 4, durationOfBreak)
      .handle(ArithmeticException.class)
      .clock(clock)
      .build();

    // Four of four actions in this test throw handled failure.
    final var throwable1 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state1 = breaker.getState();
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    // Placing the rest of invocations ('samplingDuration' / 2) + 1 seconds later ensures that even if there are only
    // two windows, then the invocations are placed in the second. They are still placed within same sampling duration.
    final var anotherWindowDuration = samplingDuration.dividedBy(2L).plusSeconds(1L);
    clock.setInstant(instant.plus(anotherWindowDuration));
    final var throwable4 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state4 = breaker.getState();
    clock.setInstant(instant.plus(durationOfBreak));
    final var state5 = breaker.getState();
    // Since the call that opened the circuit occurred in a later window, then the break duration must be simulated
    // as from that call.
    clock.setInstant(instant.plus(durationOfBreak).plus(anotherWindowDuration));
    // Duration has passed, circuit is now half open.
    final var state6 = breaker.getState();
    final var throwable6 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));

    assertThat(throwable1).isInstanceOf(ArithmeticException.class);
    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(state5).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(throwable6).isInstanceOf(ArithmeticException.class);
    assertThat(state6).isEqualTo(CircuitBreakerState.HALF_OPEN);
  }

  @Test
  public void shouldOpenCircuitAgainAfterTheSpecifiedDurationHasPassedIfTheNextCallRaisesAnException() {
    final var instant = Instant.now();
    final var clock = new MutableClock(instant, ZoneId.systemDefault());
    final var durationOfBreak = Duration.ofSeconds(30);
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, Duration.ofSeconds(10), 4, durationOfBreak)
      .handle(ArithmeticException.class)
      .clock(clock)
      .build();

    // Four of four actions in this test throw handled failures.
    final var throwable1 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state1 = breaker.getState();
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    final var throwable4 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state4 = breaker.getState();
    clock.setInstant(instant.plus(durationOfBreak));
    // Duration has passed, circuit is now half open.
    final var state5 = breaker.getState();
    // First call after duration raises an exception, so circuit should open again.
    final var throwable6 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state6 = breaker.getState();

    assertThat(throwable1).isInstanceOf(ArithmeticException.class);
    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(state5).isEqualTo(CircuitBreakerState.HALF_OPEN);
    assertThat(throwable6).isInstanceOf(ArithmeticException.class);
    assertThat(state6).isEqualTo(CircuitBreakerState.OPEN);
  }

  @Test
  public void shouldResetCircuitAfterTheSpecifiedDurationHasPassedIfTheNextCallDoesNotRaiseAnException()
    throws Throwable {
    final var instant = Instant.now();
    final var clock = new MutableClock(instant, ZoneId.systemDefault());
    final var durationOfBreak = Duration.ofSeconds(30);
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, Duration.ofSeconds(10), 4, durationOfBreak)
      .handle(ArithmeticException.class)
      .clock(clock)
      .build();

    // Four of four actions in this test throw handled failures.
    final var throwable1 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state1 = breaker.getState();
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    final var throwable4 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state4 = breaker.getState();
    clock.setInstant(instant.plus(durationOfBreak));
    // Duration has passed, circuit is now half open.
    final var state5 = breaker.getState();
    // First call after duration is successful, so circuit should reset.
    breaker.execute(() -> null);
    final var state6 = breaker.getState();

    assertThat(throwable1).isInstanceOf(ArithmeticException.class);
    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(state5).isEqualTo(CircuitBreakerState.HALF_OPEN);
    assertThat(state6).isEqualTo(CircuitBreakerState.CLOSED);
  }

  @Test
  public void shouldOnlyAllowSingleExecutionOnFirstEnteringHalfOpenStateTestExecutionPermitDirectly()
    throws BrokenCircuitException {
    final var instant = Instant.now();
    final var clock = new MutableClock(instant, ZoneId.systemDefault());
    final var durationOfBreak = Duration.ofSeconds(30);
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, Duration.ofSeconds(10), 2, durationOfBreak)
      .handle(ArithmeticException.class)
      .clock(clock)
      .build();

    final var throwable1 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    // Exception raised, circuit is now open.
    final var state2 = breaker.getState();
    // Break duration passes, circuit is now half open.
    clock.setInstant(instant.plus(durationOfBreak));
    final var state3 = breaker.getState();
    // onActionPreExecute() should permit first execution.
    breaker.getController().onActionPreExecute();
    final var state4 = breaker.getState();
    // onActionPreExecute() should reject second execution.
    final var throwable5 = catchThrowable(() -> breaker.getController().onActionPreExecute());
    final var state5 = breaker.getState();

    assertThat(throwable1).isInstanceOf(ArithmeticException.class);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(state3).isEqualTo(CircuitBreakerState.HALF_OPEN);
    assertThat(state4).isEqualTo(CircuitBreakerState.HALF_OPEN);
    assertThat(throwable5).isInstanceOf(BrokenCircuitException.class);
    assertThat(state5).isEqualTo(CircuitBreakerState.HALF_OPEN);
  }

  @Test
  public void shouldAllowSingleExecutionPerBreakDurationInHalfOpenStateTestExecutionPermitDirectly()
    throws BrokenCircuitException {
    final var instant = Instant.now();
    final var clock = new MutableClock(instant, ZoneId.systemDefault());
    final var durationOfBreak = Duration.ofSeconds(30);
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, Duration.ofSeconds(10), 2, durationOfBreak)
      .handle(ArithmeticException.class)
      .clock(clock)
      .build();

    final var throwable1 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    // Exception raised, circuit is now open.
    final var state2 = breaker.getState();
    // Break duration passes, circuit is now half open.
    clock.setInstant(instant.plus(durationOfBreak));
    final var state3 = breaker.getState();
    // onActionPreExecute() should permit first execution.
    breaker.getController().onActionPreExecute();
    final var state4 = breaker.getState();
    // onActionPreExecute() should reject second execution.
    final var throwable5 = catchThrowable(() -> breaker.getController().onActionPreExecute());
    final var state5 = breaker.getState();
    // Allow another time window to pass. Breaker should still be half open.
    clock.setInstant(instant.plus(durationOfBreak.multipliedBy(2L)));
    final var state6 = breaker.getState();
    // onActionPreExecute() should now permit another trial execution.
    breaker.getController().onActionPreExecute();
    final var state7 = breaker.getState();

    assertThat(throwable1).isInstanceOf(ArithmeticException.class);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(state3).isEqualTo(CircuitBreakerState.HALF_OPEN);
    assertThat(state4).isEqualTo(CircuitBreakerState.HALF_OPEN);
    assertThat(throwable5).isInstanceOf(BrokenCircuitException.class);
    assertThat(state5).isEqualTo(CircuitBreakerState.HALF_OPEN);
    assertThat(state6).isEqualTo(CircuitBreakerState.HALF_OPEN);
    assertThat(state7).isEqualTo(CircuitBreakerState.HALF_OPEN);
  }

  @Test
  public void shouldOnlyAllowSingleExecutionOnFirstEnteringHalfOpenStateIntegrationTest() throws InterruptedException {
    final var instant = Instant.now();
    final var clock = new MutableClock(instant, ZoneId.systemDefault());
    final var durationOfBreak = Duration.ofSeconds(30);
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, Duration.ofSeconds(10), 2, durationOfBreak)
      .handle(ArithmeticException.class)
      .clock(clock)
      .build();

    final var throwable1 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    // Exception raised, circuit is now open.
    final var state2 = breaker.getState();
    // Break duration passes, circuit is now half open.
    clock.setInstant(instant.plus(durationOfBreak));
    final var state3 = breaker.getState();
    // Start one execution during the half open state and request a second execution before the first has completed
    // (ie still during the half open state). The second execution should be rejected due to the half open state.
    final var testTimeoutToExposeDeadlocksSec = 5;
    final var permitSecondExecutionAttempt = new Semaphore(0);
    final var permitFirstExecutionEnd = new Semaphore(0);
    final var firstDelegateExecutedInHalfOpenState = new AtomicBoolean();
    final var secondDelegateExecutedInHalfOpenState = new AtomicBoolean();
    final var secondDelegateRejectedInHalfOpenState = new AtomicBoolean();
    final var firstExecutionActive = new AtomicBoolean();
    // First execution in half open state: we should be able to verify state is half open as it executes.
    final var firstExecution = new Thread(() -> {
      try {
        breaker.execute(() -> {
          firstDelegateExecutedInHalfOpenState.set(breaker.getState() == CircuitBreakerState.HALF_OPEN);
          // Signal the second execution can start, overlapping with this (the first) execution.
          firstExecutionActive.set(true);
          permitSecondExecutionAttempt.release();
          // Hold first execution open until second indicates it is no longer needed, or time out.
          if (permitFirstExecutionEnd.tryAcquire(testTimeoutToExposeDeadlocksSec, TimeUnit.SECONDS)) {
            permitFirstExecutionEnd.release();
          }
          firstExecutionActive.set(false);
          return null;
        });
      } catch (Throwable e) {
        throw new RuntimeException(e);
      }
    });
    firstExecution.start();
    // Attempt a second execution, signalled by the first execution to ensure they overlap: we should be able to
    // verify it doesn't execute and is rejected by a breaker in a half open state.
    if (permitSecondExecutionAttempt.tryAcquire(testTimeoutToExposeDeadlocksSec, TimeUnit.SECONDS)) {
      permitSecondExecutionAttempt.release();
    }
    final var secondExecution = new Thread(() -> {
      // Validation of correct sequencing and overlapping of tasks in test (guard against erroneous test
      // refactorings/operation).
      assertThat(firstExecutionActive.get()).isTrue();
      assertThat(breaker.getState()).isEqualTo(CircuitBreakerState.HALF_OPEN);
      try {
        breaker.execute(() -> {
          secondDelegateRejectedInHalfOpenState.set(false);
          secondDelegateExecutedInHalfOpenState.set(breaker.getState() == CircuitBreakerState.HALF_OPEN);
          return null;
        });
      } catch (BrokenCircuitException e) {
        secondDelegateExecutedInHalfOpenState.set(false);
        secondDelegateRejectedInHalfOpenState.set(breaker.getState() == CircuitBreakerState.HALF_OPEN);
      } catch (Throwable e) {
        throw new RuntimeException(e);
      }
      // Release first execution soon as second overlapping execution is done gathering data.
      permitFirstExecutionEnd.release();
    });
    secondExecution.start();
    // Graceful cleanup: allow executions time to end naturally; signal them to end if not; timeout any deadlocks;
    // expose any execution faults. This validates the test ran as expected (and background delegates are complete)
    // before we assert outcomes.
    if (permitFirstExecutionEnd.tryAcquire(testTimeoutToExposeDeadlocksSec, TimeUnit.SECONDS)) {
      permitFirstExecutionEnd.release();
    }
    var done = firstExecution.join(Duration.ofSeconds(testTimeoutToExposeDeadlocksSec));
    done = done && secondExecution.join(Duration.ofSeconds(testTimeoutToExposeDeadlocksSec));
    assertThat(done).isTrue();

    assertThat(throwable1).isInstanceOf(ArithmeticException.class);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(state3).isEqualTo(CircuitBreakerState.HALF_OPEN);
    // Assert:
    // - first execution should have been permitted and executed under a half open state
    // - second overlapping execution is half open state should not have been permitted
    // - second execution attempt should have been rejected with half open state as cause
    assertThat(firstDelegateExecutedInHalfOpenState.get()).isTrue();
    assertThat(secondDelegateExecutedInHalfOpenState.get()).isFalse();
    assertThat(secondDelegateRejectedInHalfOpenState.get()).isTrue();
  }

  @Test
  public void shouldAllowSingleExecutionPerBreakDurationInHalfOpenStateIntegrationTest() throws InterruptedException {
    final var instant = Instant.now();
    final var clock = new MutableClock(instant, ZoneId.systemDefault());
    final var durationOfBreak = Duration.ofSeconds(30);
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, Duration.ofSeconds(10), 2, durationOfBreak)
      .handle(ArithmeticException.class)
      .clock(clock)
      .build();

    final var throwable1 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    // Exception raised, circuit is now open.
    final var state2 = breaker.getState();
    // Break duration passes, circuit is now half open.
    clock.setInstant(instant.plus(durationOfBreak));
    final var state3 = breaker.getState();
    // Start one execution during the half open state. Request a second execution while the first in flight (not
    // completed), while still during half open state, but after one break duration later. The second execution
    // should be accepted in the half open state due to being requested after on break duration later.
    final var testTimeoutToExposeDeadlocksSec = 5;
    final var permitSecondExecutionAttempt = new Semaphore(0);
    final var permitFirstExecutionEnd = new Semaphore(0);
    final var firstDelegateExecutedInHalfOpenState = new AtomicBoolean();
    final var secondDelegateExecutedInHalfOpenState = new AtomicBoolean();
    final var secondDelegateRejectedInHalfOpenState = new AtomicBoolean();
    final var firstExecutionActive = new AtomicBoolean();
    // First execution in half open state: we should be able to verify state is half open as it executes.
    final var firstExecution = new Thread(() -> {
      try {
        breaker.execute(() -> {
          firstDelegateExecutedInHalfOpenState.set(breaker.getState() == CircuitBreakerState.HALF_OPEN);
          // Signal the second execution can start, overlapping with this (the first) execution.
          firstExecutionActive.set(true);
          permitSecondExecutionAttempt.release();
          // Hold first execution open until second indicates it is no longer needed, or time out.
          if (permitFirstExecutionEnd.tryAcquire(testTimeoutToExposeDeadlocksSec, TimeUnit.SECONDS)) {
            permitFirstExecutionEnd.release();
          }
          firstExecutionActive.set(false);
          return null;
        });
      } catch (Throwable e) {
        throw new RuntimeException(e);
      }
    });
    firstExecution.start();
    // Attempt a second execution, signalled by the first execution to ensure they overlap, start it one break
    // duration later: we should be able to verify it doesn't execute and is rejected by a breaker in a half open state.
    if (permitSecondExecutionAttempt.tryAcquire(testTimeoutToExposeDeadlocksSec, TimeUnit.SECONDS)) {
      permitSecondExecutionAttempt.release();
    }
    final var secondExecution = new Thread(() -> {
      // Validation of correct sequencing and overlapping of tasks in test (guard against erroneous test
      // refactorings/operation).
      assertThat(firstExecutionActive.get()).isTrue();
      assertThat(breaker.getState()).isEqualTo(CircuitBreakerState.HALF_OPEN);
      clock.setInstant(instant.plus(durationOfBreak.multipliedBy(2L)));
      try {
        breaker.execute(() -> {
          secondDelegateRejectedInHalfOpenState.set(false);
          secondDelegateExecutedInHalfOpenState.set(breaker.getState() == CircuitBreakerState.HALF_OPEN);
          return null;
        });
      } catch (BrokenCircuitException e) {
        secondDelegateExecutedInHalfOpenState.set(false);
        secondDelegateRejectedInHalfOpenState.set(breaker.getState() == CircuitBreakerState.HALF_OPEN);
      } catch (Throwable e) {
        throw new RuntimeException(e);
      }
      // Release first execution soon as second overlapping execution is done gathering data.
      permitFirstExecutionEnd.release();
    });
    secondExecution.start();
    // Graceful cleanup: allow executions time to end naturally; signal them to end if not; timeout any deadlocks;
    // expose any execution faults. This validates the test ran as expected (and background delegates are complete)
    // before we assert outcomes.
    if (permitFirstExecutionEnd.tryAcquire(testTimeoutToExposeDeadlocksSec, TimeUnit.SECONDS)) {
      permitFirstExecutionEnd.release();
    }
    var done = firstExecution.join(Duration.ofSeconds(testTimeoutToExposeDeadlocksSec));
    done = done && secondExecution.join(Duration.ofSeconds(testTimeoutToExposeDeadlocksSec));
    assertThat(done).isTrue();

    assertThat(throwable1).isInstanceOf(ArithmeticException.class);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(state3).isEqualTo(CircuitBreakerState.HALF_OPEN);
    // Assert:
    // - first execution should have been permitted and executed under a half open state
    // - second overlapping execution is half open state should not have been permitted, one break duration later
    assertThat(firstDelegateExecutedInHalfOpenState.get()).isTrue();
    assertThat(secondDelegateExecutedInHalfOpenState.get()).isTrue();
    assertThat(secondDelegateRejectedInHalfOpenState.get()).isFalse();
  }
  //</editor-fold>

  //<editor-fold desc="isolate and reset tests">
  @Test
  public void shouldOpenCircuitAndBlockCallsIfManualOverrideOpen() {
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, Duration.ofSeconds(10), 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .build();

    final var state1 = breaker.getState();
    // Manually break circuit.
    breaker.isolate();
    final var state2 = breaker.getState();
    // Circuit manually broken. Execution should be blocked, even non-exception-throwing executions should not reset
    // circuit.
    final var delegateExecutedWhenBroken = new AtomicBoolean();
    final var throwable3 = catchThrowable(() -> breaker.execute(() -> {
      delegateExecutedWhenBroken.set(true);
      return null;
    }));
    final var state3 = breaker.getState();

    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(state2).isEqualTo(CircuitBreakerState.ISOLATED);
    assertThat(throwable3).isInstanceOf(IsolatedCircuitBreakerException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.ISOLATED);
    assertThat(breaker.getLastOutcome().isPresent()).isTrue();
    assertThat(breaker.getLastOutcome().get()).isInstanceOf(DelegateResult.Failure.class);
    final var f = (DelegateResult.Failure<Void>) breaker.getLastOutcome().get();
    assertThat(f.getException()).isInstanceOf(IsolatedCircuitBreakerException.class);
    assertThat(delegateExecutedWhenBroken.get()).isFalse();
  }

  @Test
  public void shouldHoldCircuitOpenDespiteElapsedTimeIfManualOverrideOpen() {
    final var instant = Instant.now();
    final var clock = new MutableClock(instant, ZoneId.systemDefault());
    final var durationOfBreak = Duration.ofSeconds(30);
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, Duration.ofSeconds(10), 4, durationOfBreak)
      .handle(ArithmeticException.class)
      .clock(clock)
      .build();

    final var state1 = breaker.getState();
    breaker.isolate();
    final var state2 = breaker.getState();
    clock.setInstant(instant.plus(durationOfBreak));
    final var state3 = breaker.getState();
    final var delegateExecutedWhenBroken = new AtomicBoolean();
    final var throwable4 = catchThrowable(() -> breaker.execute(() -> {
      delegateExecutedWhenBroken.set(true);
      return null;
    }));

    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(state2).isEqualTo(CircuitBreakerState.ISOLATED);
    assertThat(state3).isEqualTo(CircuitBreakerState.ISOLATED);
    assertThat(throwable4).isInstanceOf(IsolatedCircuitBreakerException.class);
    assertThat(delegateExecutedWhenBroken.get()).isFalse();
  }

  @Test
  public void shouldCloseCircuitAgainOnResetAfterManualOverride() throws Throwable {
    final var breaker = CircuitBreakerPolicy
      .<Void>advancedBuilder(0.5, Duration.ofSeconds(10), 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .build();

    final var state1 = breaker.getState();
    breaker.isolate();
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> breaker.execute(() -> null));
    breaker.reset();
    final var state4 = breaker.getState();
    breaker.execute(() -> null);

    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(state2).isEqualTo(CircuitBreakerState.ISOLATED);
    assertThat(throwable3).isInstanceOf(IsolatedCircuitBreakerException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.CLOSED);
  }

  @Test
  public void shouldBeAbleToResetAutomaticallyOpenedCircuitWithoutSpecifiedDurationPassing() throws Throwable {
    final var breaker = CircuitBreakerPolicy
      .advancedBuilder(0.5, Duration.ofSeconds(10), 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .build();

    // Four of four actions in this test throw handled failures.
    final var throwable1 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state1 = breaker.getState();
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    final var throwable4 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state4 = breaker.getState();
    // Reset circuit with no time having passed.
    breaker.reset();
    final var state5 = breaker.getState();
    breaker.execute(() -> null);

    assertThat(throwable1).isInstanceOf(ArithmeticException.class);
    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(state5).isEqualTo(CircuitBreakerState.CLOSED);
  }
  //</editor-fold>

  //<editor-fold desc="state-change delegate tests">
  @Test
  public void shouldNotCallOnResetOnInitialise() {
    final var onResetCalled = new AtomicBoolean();
    CircuitBreakerPolicy
      .advancedBuilder(0.5, Duration.ofSeconds(10), 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .onReset(ctx -> onResetCalled.set(true))
      .build();

    assertThat(onResetCalled.get()).isFalse();
  }

  @Test
  public void shouldNotCallOnBreakOnInitialise() {
    final var onBreakCalled = new AtomicBoolean();
    CircuitBreakerPolicy
      .advancedBuilder(0.5, Duration.ofSeconds(10), 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .onBreak(event -> onBreakCalled.set(true))
      .build();

    assertThat(onBreakCalled.get()).isFalse();
  }

  @Test
  public void shouldNotCallOnHalfOpenInitialise() {
    final var onHalfOpenCalled = new AtomicBoolean();
    CircuitBreakerPolicy
      .advancedBuilder(0.5, Duration.ofSeconds(10), 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .onHalfOpen(() -> onHalfOpenCalled.set(true))
      .build();

    assertThat(onHalfOpenCalled.get()).isFalse();
  }

  @Test
  public void shouldCallOnBreakWhenBreakingCircuitAutomatically() {
    final var onBreakCalled = new AtomicBoolean();
    final var breaker = CircuitBreakerPolicy
      .advancedBuilder(0.5, Duration.ofSeconds(10), 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .onBreak(event -> onBreakCalled.set(true))
      .build();

    // Four of four actions in this test throw handled failures.
    final var throwable1 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state1 = breaker.getState();
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    // No adjustment to clock, so all exceptions raised within same sampling duration.
    final var throwable4 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state4 = breaker.getState();

    assertThat(throwable1).isInstanceOf(ArithmeticException.class);
    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(onBreakCalled.get()).isTrue();
  }

  @Test
  public void shouldCallOnBreakWhenBreakingCircuitManually() {
    final var onBreakCalled = new AtomicBoolean();
    final var breaker = CircuitBreakerPolicy
      .advancedBuilder(0.5, Duration.ofSeconds(10), 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .onBreak(event -> onBreakCalled.set(true))
      .build();

    breaker.isolate();

    assertThat(onBreakCalled.get()).isTrue();
  }

  @Test
  public void shouldCallOnBreakWhenBreakingCircuitFirstTimeButNotForSubsequentCallsPlacedThroughOpenCircuit() {
    final var onBreakCalled = new AtomicInteger();
    final var breaker = CircuitBreakerPolicy
      .advancedBuilder(0.5, Duration.ofSeconds(10), 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .onBreak(event -> onBreakCalled.incrementAndGet())
      .build();

    // Four of four actions in this test throw handled failures.
    final var throwable1 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state1 = breaker.getState();
    final var onBreak1 = onBreakCalled.get();
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state2 = breaker.getState();
    final var onBreak2 = onBreakCalled.get();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    final var onBreak3 = onBreakCalled.get();
    // No adjustment to clock, so all exceptions raised within same sampling duration.
    final var throwable4 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state4 = breaker.getState();
    final var onBreak4 = onBreakCalled.get();
    // Call through circuit when already broken should not re-trigger onBreak.
    final var throwable5 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state5 = breaker.getState();
    final var onBreak5 = onBreakCalled.get();

    assertThat(throwable1).isInstanceOf(ArithmeticException.class);
    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(onBreak1).isEqualTo(0);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(onBreak2).isEqualTo(0);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(onBreak3).isEqualTo(0);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(onBreak4).isEqualTo(1);
    assertThat(throwable5).isInstanceOf(BrokenCircuitException.class);
    assertThat(state5).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(onBreak5).isEqualTo(1);
  }

  @Test
  public void shouldCallOnBreakWhenBreakingCircuitFirstTimeButNotForSubsequentCallFailureWhichArrivesOnOpenStateThoughStartedOnClosedState() throws InterruptedException {
    final var onBreakCalled = new AtomicInteger();
    final var breaker = CircuitBreakerPolicy
      .advancedBuilder(0.5, Duration.ofSeconds(10), 2, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .onBreak(event -> onBreakCalled.incrementAndGet())
      .build();

    // Start an execution when the breaker is in the closed state, but hold it from returning (its failure) until the
    // breaker has opened. This call, a failure hitting an already open breaker, should indicate its fail, but should
    // not cause onBreak to be called a second time.
    final var testTimeoutToExposeDeadlocksSec = 5;
    final var permitLongRunningExecutionToReturnItsFailure = new Semaphore(0);
    final var permitMainThreadToOpenCircuit = new Semaphore(0);
    final var longRunningExecution = new Thread(() -> {
      assertThat(breaker.getState()).isEqualTo(CircuitBreakerState.CLOSED);
      final var throwable = catchThrowable(() -> breaker.execute(() -> {
        permitMainThreadToOpenCircuit.release();
        // Hold this execution until rest of the test indicates it can proceed (or timeout, to expose deadlocks).
        if (permitLongRunningExecutionToReturnItsFailure.tryAcquire(testTimeoutToExposeDeadlocksSec,
          TimeUnit.SECONDS)) {
          permitLongRunningExecutionToReturnItsFailure.release();
        }
        // Throw a further failure when rest of test has already broken the circuit.
        assertThat(breaker.getState()).isEqualTo(CircuitBreakerState.OPEN);
        throw new ArithmeticException();
      }));
      // However, since execution started when circuit was closed, BrokenCircuitException will not have been throw on
      // entry. The original exception will still be thrown.
      assertThat(throwable).isInstanceOf(ArithmeticException.class);
    });
    longRunningExecution.start();
    if (permitMainThreadToOpenCircuit.tryAcquire(testTimeoutToExposeDeadlocksSec, TimeUnit.SECONDS)) {
      permitMainThreadToOpenCircuit.release();
    }
    // Break circuit in the normal manner: onBreak should be called once.
    final var state1 = breaker.getState();
    final var onBreak1 = onBreakCalled.get();
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    final var onBreak3 = onBreakCalled.get();
    // Permit the second (long-running) execution to hit the open circuit with its failure.
    permitLongRunningExecutionToReturnItsFailure.release();
    // Graceful cleanup: allow executions time to end naturally; timeout if any deadlocks; expose any execution
    // faults. This validates the test ran as expected (and background delegates are complete) before we assert an
    // outcome.
    final var done = longRunningExecution.join(Duration.ofSeconds(testTimeoutToExposeDeadlocksSec));
    final var state4 = breaker.getState();
    final var onBreak4 = onBreakCalled.get();

    assertThat(done).isTrue();
    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(onBreak1).isEqualTo(0);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(onBreak3).isEqualTo(1);
    // onBreak should still only have been called once.
    assertThat(state4).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(onBreak4).isEqualTo(1);
  }

  @Test
  public void shouldCallOnResetWhenAutomaticallyClosingCircuitButNotWhenHalfOpen() throws Throwable {
    final var onBreakCalled = new AtomicInteger();
    final var onResetCalled = new AtomicInteger();
    final var instant = Instant.now();
    final var clock = new MutableClock(instant, ZoneId.systemDefault());
    final var durationOfBreak = Duration.ofSeconds(30);
    final var breaker = CircuitBreakerPolicy
      .advancedBuilder(0.5, Duration.ofSeconds(10), 4, durationOfBreak)
      .handle(ArithmeticException.class)
      .onBreak(event -> onBreakCalled.incrementAndGet())
      .onReset(ctx -> onResetCalled.incrementAndGet())
      .clock(clock)
      .build();

    // Four of four actions in this test throw handled failures.
    final var throwable1 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state1 = breaker.getState();
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    // No adjustment to clock, so all exceptions raised within same sampling duration.
    final var throwable4 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state4 = breaker.getState();
    final var onBreak4 = onBreakCalled.get();
    clock.setInstant(instant.plus(durationOfBreak));
    // Duration has passed, circuit is now half open, but not yet reset.
    final var state5 = breaker.getState();
    final var onReset5 = onResetCalled.get();
    // First call after duration is successful, so circuit should reset.
    breaker.execute(() -> null);
    final var state6 = breaker.getState();
    final var onReset6 = onResetCalled.get();

    assertThat(throwable1).isInstanceOf(ArithmeticException.class);
    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(onBreak4).isEqualTo(1);
    assertThat(state5).isEqualTo(CircuitBreakerState.HALF_OPEN);
    assertThat(onReset5).isEqualTo(0);
    assertThat(state6).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(onReset6).isEqualTo(1);
  }

  @Test
  public void shouldNotCallOnResetOnSuccessiveSuccessfulCalls() throws Throwable {
    final var onResetCalled = new AtomicBoolean();
    final var breaker = CircuitBreakerPolicy
      .advancedBuilder(0.5, Duration.ofSeconds(10), 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .onReset(ctx -> onResetCalled.set(true))
      .build();

    breaker.execute(() -> null);
    final var state1 = breaker.getState();
    final var onResetCalled1 = onResetCalled.get();
    breaker.execute(() -> null);
    final var state2 = breaker.getState();
    final var onResetCalled2 = onResetCalled.get();

    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(onResetCalled1).isFalse();
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(onResetCalled2).isFalse();
  }

  @Test
  public void shouldCallOnHalfOpenWhenAutomaticallyTransitioningToHalfOpenDueToSubsequentExecution() throws Throwable {
    final var onHalfOpenCalled = new AtomicInteger();
    final var instant = Instant.now();
    final var clock = new MutableClock(instant, ZoneId.systemDefault());
    final var durationOfBreak = Duration.ofSeconds(30);
    final var breaker = CircuitBreakerPolicy
      .advancedBuilder(0.5, Duration.ofSeconds(10), 4, durationOfBreak)
      .handle(ArithmeticException.class)
      .onHalfOpen(onHalfOpenCalled::incrementAndGet)
      .clock(clock)
      .build();

    // Four of four actions in this test throw handled failures.
    final var throwable1 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state1 = breaker.getState();
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    // No adjustment to clock, so all exception raised within same sampling duration.
    final var throwable4 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state4 = breaker.getState();
    clock.setInstant(instant.plus(durationOfBreak));
    // Duration has passed, circuit is now half open.
    // Not yet transitioned to half open, because we haven't queried state.
    final var onHalfOpenCalled5 = onHalfOpenCalled.get();
    // First call after duration is successful, so circuit should reset.
    breaker.execute(() -> null);
    final var state6 = breaker.getState();
    // onHalfOpen called as action was placed for execution.
    final var onHalfOpenCalled6 = onHalfOpenCalled.get();

    assertThat(throwable1).isInstanceOf(ArithmeticException.class);
    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(onHalfOpenCalled5).isEqualTo(0);
    assertThat(state6).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(onHalfOpenCalled6).isEqualTo(1);
  }

  @Test
  public void shouldCallOnHalfOpenWhenAutomaticallyTransitioningToHalfOpenDueToStateRead() {
    final var onHalfOpenCalled = new AtomicInteger();
    final var instant = Instant.now();
    final var clock = new MutableClock(instant, ZoneId.systemDefault());
    final var durationOfBreak = Duration.ofSeconds(30);
    final var breaker = CircuitBreakerPolicy
      .advancedBuilder(0.5, Duration.ofSeconds(10), 4, durationOfBreak)
      .handle(ArithmeticException.class)
      .onHalfOpen(onHalfOpenCalled::incrementAndGet)
      .clock(clock)
      .build();

    // Four of four actions in this test throw handled failures.
    final var throwable1 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state1 = breaker.getState();
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    // No adjustment to clock, so all exception raised within same sampling duration.
    final var throwable4 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state4 = breaker.getState();
    clock.setInstant(instant.plus(durationOfBreak));
    // Duration has passed, circuit is now half open.
    final var state5 = breaker.getState();

    assertThat(throwable1).isInstanceOf(ArithmeticException.class);
    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(state5).isEqualTo(CircuitBreakerState.HALF_OPEN);
    assertThat(onHalfOpenCalled.get()).isEqualTo(1);
  }

  @Test
  public void shouldCallOnResetWhenManuallyResettingCircuit() throws Throwable {
    final var onResetCalled = new AtomicInteger();
    final var instant = Instant.now();
    final var clock = Clock.fixed(instant, ZoneId.systemDefault());
    final var breaker = CircuitBreakerPolicy
      .advancedBuilder(0.5, Duration.ofSeconds(10), 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .onReset(ctx -> onResetCalled.incrementAndGet())
      .clock(clock)
      .build();

    breaker.isolate();
    final var state1 = breaker.getState();
    final var throwable2 = catchThrowable(() -> breaker.execute(() -> null));
    final var onResetCalled2 = onResetCalled.get();
    breaker.reset();
    final var onResetCalled3 = onResetCalled.get();
    final var state3 = breaker.getState();
    breaker.execute(() -> null);

    assertThat(state1).isEqualTo(CircuitBreakerState.ISOLATED);
    assertThat(throwable2).isInstanceOf(IsolatedCircuitBreakerException.class);
    assertThat(onResetCalled2).isEqualTo(0);
    assertThat(onResetCalled3).isEqualTo(1);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
  }

  //<editor-fold desc="tests of supplied parameters to onBreak delegate">
  @Test
  public void shouldCallOnBreakWithTheLastRaisedException() {
    final var passedException = new AtomicReference<Throwable>();
    final var breaker = CircuitBreakerPolicy
      .advancedBuilder(0.5, Duration.ofSeconds(10), 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .onBreak(event -> event.outcome().onFailure(passedException::set))
      .build();

    final var throwable1 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state1 = breaker.getState();
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    final var throwable4 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state4 = breaker.getState();

    assertThat(throwable1).isInstanceOf(ArithmeticException.class);
    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(passedException.get()).isInstanceOf(ArithmeticException.class);
  }

  @Test
  public void shouldCallOnBreakWithTheStateOfClosed() {
    final var transitionedState = new AtomicReference<CircuitBreakerState>();
    final var breaker = CircuitBreakerPolicy
      .advancedBuilder(0.5, Duration.ofSeconds(10), 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .onBreak(event -> transitionedState.set(event.state()))
      .build();

    final var throwable1 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state1 = breaker.getState();
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    final var throwable4 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state4 = breaker.getState();

    assertThat(throwable1).isInstanceOf(ArithmeticException.class);
    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(transitionedState.get()).isEqualTo(CircuitBreakerState.CLOSED);
  }

  @Test
  public void shouldCallOnBreakWithTheStateOfHalfOpen() {
    final var transitionedStates = new ArrayList<CircuitBreakerState>();
    final var instant = Instant.now();
    final var clock = new MutableClock(instant, ZoneId.systemDefault());
    final var durationOfBreak = Duration.ofSeconds(30);
    final var breaker = CircuitBreakerPolicy
      .advancedBuilder(0.5, Duration.ofSeconds(10), 4, durationOfBreak)
      .handle(ArithmeticException.class)
      .onBreak(event -> transitionedStates.add(event.state()))
      .clock(clock)
      .build();

    // Four of four actions in this test throw handled failures.
    final var throwable1 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state1 = breaker.getState();
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    final var throwable4 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state4 = breaker.getState();
    clock.setInstant(instant.plus(durationOfBreak));
    // Duration has passed, circuit is now half open.
    final var state5 = breaker.getState();
    // First call after duration raises an exception, so circuit should open again.
    final var throwable6 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state6 = breaker.getState();


    assertThat(throwable1).isInstanceOf(ArithmeticException.class);
    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(state5).isEqualTo(CircuitBreakerState.HALF_OPEN);
    assertThat(throwable6).isInstanceOf(ArithmeticException.class);
    assertThat(state6).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(transitionedStates).containsExactly(CircuitBreakerState.CLOSED, CircuitBreakerState.HALF_OPEN);
  }

  @Test
  public void shouldCallOnBreakWithTheCorrectDuration() {
    final var passedBreakDuration = new AtomicReference<Duration>();
    final var durationOfBreak = Duration.ofSeconds(30);
    final var breaker = CircuitBreakerPolicy
      .advancedBuilder(0.5, Duration.ofSeconds(10), 4, durationOfBreak)
      .handle(ArithmeticException.class)
      .onBreak(event -> passedBreakDuration.set(event.durationOfBreak()))
      .build();

    final var throwable1 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state1 = breaker.getState();
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    final var throwable4 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state4 = breaker.getState();

    assertThat(throwable1).isInstanceOf(ArithmeticException.class);
    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(passedBreakDuration.get()).isEqualTo(durationOfBreak);
  }

  @Test
  public void shouldOpenCircuitWithDurationMaxValueIFManualOverrideOpen() {
    final var passedBreakDuration = new AtomicReference<Duration>();
    final var durationOfBreak = Duration.ofSeconds(30);
    final var instant = Instant.now();
    final var clock = Clock.fixed(instant, ZoneId.systemDefault());
    final var breaker = CircuitBreakerPolicy
      .advancedBuilder(0.5, Duration.ofSeconds(10), 4, durationOfBreak)
      .handle(ArithmeticException.class)
      .onBreak(event -> passedBreakDuration.set(event.durationOfBreak()))
      .clock(clock)
      .build();

    final var state1 = breaker.getState();
    breaker.isolate();
    final var state2 = breaker.getState();

    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(state2).isEqualTo(CircuitBreakerState.ISOLATED);
    assertThat(passedBreakDuration.get()).isEqualTo(Duration.ofMillis(Long.MAX_VALUE));
  }
  //</editor-fold>

  //<editor-fold desc="tests that supplied context is passed to stage-change delegates">
  @Test
  public void shouldCallOnBreakWithThePassedContext() {
    final var contextData = new AtomicReference<Map<String, Object>>();
    final var breaker = CircuitBreakerPolicy
      .advancedBuilder(0.5, Duration.ofSeconds(10), 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .onBreak(event -> contextData.set(event.context()))
      .build();

    // Four of four actions in this test throw handled failures.
    final var throwable1 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state1 = breaker.getState();
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    final var throwable4 = catchThrowable(() ->
      raiseException(breaker, Map.of("key1", "value1", "key2", "value2"), ArithmeticException::new));
    final var state4 = breaker.getState();

    assertThat(throwable1).isInstanceOf(ArithmeticException.class);
    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(contextData.get()).containsKeys("key1", "key2")
      .containsValues("value1", "value2");
  }

  @Test
  public void shouldCallOnResetWithThePassedContext() throws Throwable {
    final var contextData = new AtomicReference<Map<String, Object>>();
    final var instant = Instant.now();
    final var clock = new MutableClock(instant, ZoneId.systemDefault());
    final var durationOfBreak = Duration.ofSeconds(30);
    final var breaker = CircuitBreakerPolicy
      .advancedBuilder(0.5, Duration.ofSeconds(10), 4, durationOfBreak)
      .handle(ArithmeticException.class)
      .onReset(contextData::set)
      .clock(clock)
      .build();

    // Four of four actions in this test throw handled failures.
    final var throwable1 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state1 = breaker.getState();
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    final var throwable4 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state4 = breaker.getState();
    clock.setInstant(instant.plus(durationOfBreak));
    final var state5 = breaker.getState();
    // First call after duration should invoke onReset, with context.
    breaker.execute(Map.of("key1", "value1", "key2", "value2"), ctx -> null);
    final var state6 = breaker.getState();

    assertThat(throwable1).isInstanceOf(ArithmeticException.class);
    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(state5).isEqualTo(CircuitBreakerState.HALF_OPEN);
    assertThat(state6).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(contextData.get()).containsKeys("key1", "key2")
      .containsValues("value1", "value2");
  }

  @Test
  public void contextShouldBeEmptyIfExecuteNotCalledWithAnyContextData() {
    final var contextData = new AtomicReference<Map<String, Object>>();
    final var breaker = CircuitBreakerPolicy
      .advancedBuilder(0.5, Duration.ofSeconds(10), 4, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .onBreak(event -> contextData.set(event.context()))
      .build();

    // Four of four actions in this test throw handled failures.
    final var throwable1 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state1 = breaker.getState();
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    final var throwable4 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state4 = breaker.getState();

    assertThat(throwable1).isInstanceOf(ArithmeticException.class);
    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(contextData.get()).isEmpty();
  }

  @Test
  public void shouldCreateNewContextForEachCallToExecute() throws Throwable {
    final var contextValue = new AtomicReference<String>();
    final var instant = Instant.now();
    final var clock = new MutableClock(instant, ZoneId.systemDefault());
    final var durationOfBreak = Duration.ofSeconds(30);
    final var breaker = CircuitBreakerPolicy
      .advancedBuilder(0.5, Duration.ofSeconds(10), 4, durationOfBreak)
      .handle(ArithmeticException.class)
      .onBreak(event -> contextValue.set(event.context().containsKey("key") ? event.context().get("key").toString() :
        null))
      .onReset(ctx -> contextValue.set(ctx.containsKey("key") ? ctx.get("key").toString() : null))
      .clock(clock)
      .build();

    // Four of four actions in this test throw handled failures.
    final var throwable1 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state1 = breaker.getState();
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state3 = breaker.getState();
    final var throwable4 = catchThrowable(() ->
      raiseException(breaker, Map.of("key", "original_value"), ArithmeticException::new));
    final var state4 = breaker.getState();
    final var contextValue4 = contextValue.get();
    clock.setInstant(instant.plus(durationOfBreak));
    // Duration has passed, circuit is now half open.
    final var state5 = breaker.getState();
    // But not yet reset.
    // Fist call after duration is successful, so circuit should reset.
    breaker.execute(Map.of("key", "new_value"), ctx -> null);
    final var state6 = breaker.getState();
    final var contextValue6 = contextValue.get();

    assertThat(throwable1).isInstanceOf(ArithmeticException.class);
    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable4).isInstanceOf(ArithmeticException.class);
    assertThat(state4).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(contextValue4).isEqualTo("original_value");
    assertThat(state5).isEqualTo(CircuitBreakerState.HALF_OPEN);
    assertThat(state6).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(contextValue6).isEqualTo("new_value");
  }
  //</editor-fold>
  //</editor-fold>

  //<editor-fold desc="lastOutcome property">
  @Test
  public void shouldInitialiseLastOutcomeToEmptyOnCreation() {
    final var breaker = CircuitBreakerPolicy
      .advancedBuilder(0.5, Duration.ofSeconds(10), 2, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .build();

    assertThat(breaker.getLastOutcome()).isEmpty();
  }

  @Test
  public void shouldSetLastOutcomeFailureOnHandlingExceptionEvenWhenNotBreaking() {
    final var breaker = CircuitBreakerPolicy
      .advancedBuilder(0.5, Duration.ofSeconds(10), 2, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .build();

    final var throwable = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));

    assertThat(throwable).isInstanceOf(ArithmeticException.class);
    assertThat(breaker.getState()).isEqualTo(CircuitBreakerState.CLOSED);
    final var lastOutcome = breaker.getLastOutcome();
    assertThat(lastOutcome).isNotEmpty();
    final var lastException = lastOutcome.get().fold(o -> null, Function.identity());
    assertThat(lastException).isNotNull()
      .isInstanceOf(ArithmeticException.class);
  }

  @Test
  public void shouldSetLastOutcomeFailureToLastRaisedExceptionWhenBreaking() {
    final var breaker = CircuitBreakerPolicy
      .advancedBuilder(0.5, Duration.ofSeconds(10), 2, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .build();

    final var throwable1 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state1 = breaker.getState();
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state2 = breaker.getState();

    assertThat(throwable1).isInstanceOf(ArithmeticException.class);
    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.OPEN);
    final var lastOutcome = breaker.getLastOutcome();
    assertThat(lastOutcome).isNotEmpty();
    final var lastException = lastOutcome.get().fold(o -> null, Function.identity());
    assertThat(lastException).isNotNull()
      .isInstanceOf(ArithmeticException.class);
  }

  @Test
  public void shouldSetLastOutcomeToEmptyOnCircuitReset() {
    final var breaker = CircuitBreakerPolicy
      .advancedBuilder(0.5, Duration.ofSeconds(10), 2, Duration.ofSeconds(30))
      .handle(ArithmeticException.class)
      .build();

    final var throwable1 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state1 = breaker.getState();
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException::new));
    final var state2 = breaker.getState();
    final var lastException2 = breaker.getLastOutcome()
      .map(outcome -> outcome.fold(o -> null, Function.identity()))
      .orElse(null);
    breaker.reset();
    final var lastException3 = breaker.getLastOutcome()
      .map(outcome -> outcome.fold(o -> null, Function.identity()))
      .orElse(null);

    assertThat(throwable1).isInstanceOf(ArithmeticException.class);
    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(lastException2).isNotNull()
      .isInstanceOf(ArithmeticException.class);
    assertThat(lastException3).isNull();
  }
  //</editor-fold>
}
