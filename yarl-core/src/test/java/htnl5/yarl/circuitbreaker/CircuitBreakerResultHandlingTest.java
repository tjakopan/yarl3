package htnl5.yarl.circuitbreaker;

import htnl5.yarl.helpers.Result;
import org.junit.jupiter.api.Test;

import static htnl5.yarl.helpers.PolicyUtils.raiseResults;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class CircuitBreakerResultHandlingTest {
  //<editor-fold desc="threshold-to-break tests">
  @Test
  public void shouldNotOpenCircuitIfSpecifiedNumberOfSpecifiedHandledResultAreNotRaisedConsecutively() throws Throwable {
    final var breaker = CircuitBreakerPolicy.<Result>builder()
      .failuresAllowedBeforeBreaking(2)
      .handleResult(Result.FAULT)
      .build();

    final var result1 = raiseResults(breaker, Result.FAULT);
    final var state1 = breaker.getState();
    final var result2 = raiseResults(breaker, Result.GOOD);
    final var state2 = breaker.getState();
    final var result3 = raiseResults(breaker, Result.FAULT);
    final var state3 = breaker.getState();

    assertThat(result1).isEqualTo(Result.FAULT);
    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(result2).isEqualTo(Result.GOOD);
    assertThat(state2).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(result3).isEqualTo(Result.FAULT);
    assertThat(state3).isEqualTo(CircuitBreakerState.CLOSED);
  }

  @Test
  public void shouldOpenCircuitWithTheLastHandledResultAfterSpecifiedNumberOfSpecifiedHandledResultHaveBeenReturned()
    throws Throwable {
    final var breaker = CircuitBreakerPolicy.<Result>builder()
      .failuresAllowedBeforeBreaking(2)
      .handleResult(Result.FAULT)
      .build();

    final var result1 = raiseResults(breaker, Result.FAULT);
    final var state1 = breaker.getState();
    final var result2 = raiseResults(breaker, Result.FAULT);
    final var state2 = breaker.getState();
    final var throwable3 = catchThrowable(() -> raiseResults(breaker, Result.FAULT));
    final var state3 = breaker.getState();

    assertThat(result1).isEqualTo(Result.FAULT);
    assertThat(state1).isEqualTo(CircuitBreakerState.CLOSED);
    assertThat(result2).isEqualTo(Result.FAULT);
    assertThat(state2).isEqualTo(CircuitBreakerState.OPEN);
    assertThat(throwable3).isInstanceOf(BrokenCircuitWithResultException.class)
      .hasMessage("The circuit is now open and is not allowing calls.")
      .hasFieldOrPropertyWithValue("result", Result.FAULT);
    assertThat(state3).isEqualTo(CircuitBreakerState.OPEN);
  }
  //</editor-fold>
}
