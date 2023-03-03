package htnl5.yarl.circuitbreaker;

import htnl5.yarl.helpers.Result;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static htnl5.yarl.helpers.PolicyUtils.raiseResults;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class CircuitBreakerConfigurationTest {
  @Test
  public void shouldBeAbleToHandleADurationOfMaxValue() throws Throwable {
    final var breaker = CircuitBreakerPolicy.<Result>builder()
      .durationOfBreak(Duration.ofMillis(Long.MAX_VALUE))
      .handleResult(Result.FAULT)
      .build();

    final var result = raiseResults(breaker, Result.FAULT);

    assertThat(result).isEqualTo(Result.FAULT);
  }

  @Test
  public void shouldThrowIfFailuresAllowedBeforeBreakingIsLessThanOne() {
    final var throwable = catchThrowable(() -> CircuitBreakerPolicy.<Result>builder()
      .failuresAllowedBeforeBreaking(0)
      .durationOfBreak(Duration.ofSeconds(10))
      .handleResult(Result.FAULT)
      .build());

    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("failuresAllowedBeforeBreaking");
  }

  @Test
  public void shouldThrowIfDurationOfBreakIsLessThanZero() {
    final var throwable = catchThrowable(() -> CircuitBreakerPolicy.<Result>builder()
      .durationOfBreak(Duration.ofSeconds(-1))
      .handleResult(Result.FAULT)
      .build());

    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("durationOfBreak");
  }

  @Test
  public void shouldBeAbleToHandleADurationOfBreakOfZero() {
    CircuitBreakerPolicy.<Result>builder()
      .durationOfBreak(Duration.ZERO)
      .handleResult(Result.FAULT)
      .build();
  }

  @Test
  public void shouldInitialiseToCloseState() {
    final var breaker = CircuitBreakerPolicy.<Result>builder()
      .failuresAllowedBeforeBreaking(2)
      .handleResult(Result.FAULT)
      .build();

    assertThat(breaker.getState()).isEqualTo(CircuitBreakerState.CLOSED);
  }
}
