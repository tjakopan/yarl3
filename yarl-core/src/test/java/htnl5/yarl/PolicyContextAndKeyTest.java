package htnl5.yarl;

import htnl5.yarl.helpers.Result;
import htnl5.yarl.retry.RetryPolicy;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static htnl5.yarl.helpers.PolicyUtils.raiseResults;
import static org.assertj.core.api.Assertions.assertThat;

public class PolicyContextAndKeyTest {
  //<editor-fold desc="configuration tests">
  @Test
  public void policyKeyShouldBeTheConfiguredPolicyKey() {
    final var key = "SomePolicyKey";

    final var policy = RetryPolicy.<Integer>builder()
      .handleResult(0)
      .policyKey(key)
      .build();

    assertThat(policy.getPolicyKey()).isEqualTo(key);
  }

  @Test
  public void policyKeyShouldNotBeNullOrEmptyIfNotExplicitlyConfigured() {
    final var policy = RetryPolicy.<Integer>builder()
      .handleResult(0)
      .build();

    assertThat(policy.getPolicyKey()).isNotNull();
    assertThat(policy.getPolicyKey()).isNotEmpty();
  }

  @Test
  public void policyKeyShouldStartWithPolicyTypeIfNotExplicitlyConfigured() {
    final var policy = RetryPolicy.<Integer>builder()
      .handleResult(0)
      .build();

    assertThat(policy.getPolicyKey()).startsWith(RetryPolicy.class.getSimpleName());
  }

  @Test
  public void policyKeyShouldBeUniqueForDifferentInstancesIfNotExplicitlyConfigured() {
    final var policy1 = RetryPolicy.<Integer>builder()
      .handleResult(0)
      .build();
    final var policy2 = RetryPolicy.<Integer>builder()
      .handleResult(0)
      .build();

    assertThat(policy2.getPolicyKey()).isNotEqualTo(policy1.getPolicyKey());
  }

  @Test
  public void policyKeyShouldReturnConsistentValueForSamePolicyInstanceIfNotExplicitlyConfigured() {
    final var policy = RetryPolicy.<Integer>builder()
      .handleResult(0)
      .build();

    final var keyRetrievedFirst = policy.getPolicyKey();
    final var keyRetrievedSecond = policy.getPolicyKey();

    assertThat(keyRetrievedSecond).isEqualTo(keyRetrievedFirst);
  }
  //</editor-fold>

  //<editor-fold desc="keys and execution context tests">
  @Test
  public void shouldPassPolicyKeyToExecutionContext() throws Throwable {
    final var policyKey = UUID.randomUUID().toString();
    final var policyKeySetOnExecutionContext = new AtomicReference<String>();
    final var policy = RetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .maxRetryCount(1)
      .onRetry(event -> policyKeySetOnExecutionContext.set(event.context().getPolicyKey().orElse(null)))
      .policyKey(policyKey)
      .build();

    raiseResults(policy, Result.FAULT, Result.GOOD);

    assertThat(policyKeySetOnExecutionContext.get()).isEqualTo(policyKey);
  }

  @Test
  public void shouldPassOperationKeyToExecutionContext() throws Throwable {
    final var operationKey = "SomeKey";
    final var operationKeySetOnContext = new AtomicReference<String>();
    final var policy = RetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .maxRetryCount(1)
      .onRetry(event -> operationKeySetOnContext.set(event.context().getOperationKey().orElse(null)))
      .build();

    raiseResults(policy, new Context(operationKey), Result.FAULT, Result.GOOD);

    assertThat(operationKeySetOnContext.get()).isEqualTo(operationKey);
  }
  //</editor-fold>
}
