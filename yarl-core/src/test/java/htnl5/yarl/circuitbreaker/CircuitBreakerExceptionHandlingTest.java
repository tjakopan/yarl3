package htnl5.yarl.circuitbreaker;

import htnl5.yarl.helpers.PolicyUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static htnl5.yarl.helpers.PolicyUtils.raiseException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class CircuitBreakerExceptionHandlingTest {
  //<editor-fold desc="threshold-to-break tests">
  @Test
  public void shouldNotOpenCircuitIfSpecifiedNumberOfSpecifiedExceptionAreNotRaisedConsecutively() throws Throwable {
    final var breaker = CircuitBreakerPolicy.builder()
      .failuresAllowedBeforeBreaking(2)
      .handle(ArithmeticException.class)
      .build();

    final var throwable1 = catchThrowable(() -> raiseException(breaker, ArithmeticException.class));
    final var state1 = breaker.getState();
    breaker.execute(() -> null);
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseException(breaker, ArithmeticException.class));
    final var state3 = breaker.getState();

    assertThat(throwable1).isInstanceOf(ArithmeticException.class);
    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable3).isInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
  }

  @Test
  public void shouldOpenCircuitBlockingExecutionsAndNotingTheLasRaisedExceptionAfterSpecifiedNumberOfOneOfTheSpecifiedExceptionsHaveBeenRaised() {
    final var breaker = CircuitBreakerPolicy.builder()
      .failuresAllowedBeforeBreaking(2)
      .handle(ArithmeticException.class)
      .build();
    final var delegateExecutedWhenBroken = new AtomicBoolean();

    final var throwable1 = catchThrowable(() -> raiseException(breaker, ArithmeticException.class));
    final var state1 = breaker.getState();
    final var throwable2 = catchThrowable(() -> raiseException(breaker, ArithmeticException.class));
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> breaker.execute(() -> {
      delegateExecutedWhenBroken.set(true);
      return null;
    }));
    final var state3 = breaker.getState();

    assertThat(throwable1).isInstanceOf(ArithmeticException.class);
    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(throwable2).isInstanceOf(ArithmeticException.class);
    assertThat(state2).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(throwable3).isInstanceOf(BrokenCircuitException.class)
      .hasMessage("The circuit is now open and is not allowing calls.")
      .hasCauseInstanceOf(ArithmeticException.class);
    assertThat(state3).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(delegateExecutedWhenBroken.get()).isFalse();
  }
  //</editor-fold>
}
