package htnl5.yarl;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ContextTest {
  @Test
  public void shouldAssignOperationKeyFromConstructor() {
    final var context = new Context("SomeKey");

    assertThat(context.getOperationKey().orElse(null)).isEqualTo("SomeKey");
    assertThat(context.keySet()).isEmpty();
  }

  @Test
  public void shouldAssignOperationKeyAndContextDataFromConstructor() {
    final var context = new Context("SomeKey", Map.of("key1", "value1", "key2", "value2"));

    assertThat(context.getOperationKey().orElse(null)).isEqualTo("SomeKey");
    assertThat(context.get("key1")).isEqualTo("value1");
    assertThat(context.get("key2")).isEqualTo("value2");
  }

  @Test
  public void noArgsConstructorShouldAssignNoOperationKey() {
    //noinspection MismatchedQueryAndUpdateOfCollection
    final var context = new Context();

    assertThat(context.getOperationKey()).isNotPresent();
  }

  @Test
  public void shouldAssignCorrelationIdWhenAccessed() {
    //noinspection MismatchedQueryAndUpdateOfCollection
    final var context = new Context("SomeKey");

    assertThat(context.getCorrelationId()).isNotNull();
  }

  @Test
  public void shouldReturnConsistentCorrelationId() {
    //noinspection MismatchedQueryAndUpdateOfCollection
    final var context = new Context("SomeKey");
    final var retrieved1 = context.getCorrelationId();

    final var retrieved2 = context.getCorrelationId();

    assertThat(retrieved2).isEqualTo(retrieved1);
  }
}
