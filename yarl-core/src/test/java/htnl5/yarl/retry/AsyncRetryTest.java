package htnl5.yarl.retry;

import htnl5.yarl.helpers.Result;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class AsyncRetryTest {
  @Test
  public void shouldThrowWhenMaxRetryCountIsLessThanZero() {
    final var throwable = catchThrowable(() -> AsyncRetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .maxRetryCount(-1));

    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("maxRetryCount");
  }

  @Test
  public void shouldThrowWhenExecutorIsNull() {
    final var throwable = catchThrowable(() -> AsyncRetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .executor(null));

    assertThat(throwable).isInstanceOf(NullPointerException.class)
      .hasMessageContaining("executor");
  }

  @Test
  public void shouldThrowWhenOnRetryAsyncActionIsNull() {
    final var throwable = catchThrowable(() -> AsyncRetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .onRetryAsync(null));

    assertThat(throwable).isInstanceOf(NullPointerException.class)
      .hasMessageContaining("onRetryAsync");
  }

  @Test
  public void shouldThrowWhenOnRetryActionIsNull() {
    final var throwable = catchThrowable(() -> AsyncRetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .onRetry(null));

    assertThat(throwable).isInstanceOf(NullPointerException.class)
      .hasMessageContaining("onRetry");
  }
}
