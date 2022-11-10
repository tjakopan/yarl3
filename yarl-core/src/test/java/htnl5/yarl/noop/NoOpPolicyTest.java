package htnl5.yarl.noop;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NoOpPolicyTest {
  @Test
  public void shouldExecuteAction() throws Throwable {
    final var policy = NoOpPolicy.<Integer>build();

    final var result = policy.execute(() -> 10);

    assertThat(result).isEqualTo(10);
  }
}
