package htnl5.yarl.wrap;

import htnl5.yarl.circuitbreaker.CircuitBreakerPolicy;
import htnl5.yarl.fallback.FallbackPolicy;
import htnl5.yarl.helpers.Result;
import htnl5.yarl.retry.RetryPolicy;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static htnl5.yarl.helpers.PolicyUtils.raiseResults;
import static org.assertj.core.api.Assertions.assertThat;

public class PolicyWrapContextAndKeyTest {
  @Test
  public void shouldPassPolicyKeyToExecutionContextOfOuterPolicyAsPolicyWrapKey() throws Throwable {
    final var retryKey = UUID.randomUUID().toString();
    final var breakerKey = UUID.randomUUID().toString();
    final var wrapKey = UUID.randomUUID().toString();
    final var policyWrapKeySetOnContext = new AtomicReference<String>();
    final var retry = RetryPolicy.<Result>builder()
      .policyKey(retryKey)
      .handleResult(Result.FAULT)
      .maxRetryCount(1)
      .onRetry(event -> policyWrapKeySetOnContext.set(event.context().getPolicyWrapKey().orElse(null)))
      .build();
    final var breaker = CircuitBreakerPolicy.<Result>builder()
      .policyKey(breakerKey)
      .handleResult(Result.FAULT)
      .durationOfBreak(Duration.ZERO)
      .build();
    final var wrap = PolicyWrap.wrap(wrapKey, retry, breaker);

    raiseResults(wrap, Result.FAULT, Result.GOOD);

    assertThat(policyWrapKeySetOnContext.get()).isNotEqualTo(retryKey);
    assertThat(policyWrapKeySetOnContext.get()).isNotEqualTo(breakerKey);
    assertThat(policyWrapKeySetOnContext.get()).isEqualTo(wrapKey);
  }

  @Test
  public void shouldPassPolicyKeyToExecutionContextOfInnerPolicyAsPolicyWrapKey() throws Throwable {
    final var retryKey = UUID.randomUUID().toString();
    final var breakerKey = UUID.randomUUID().toString();
    final var wrapKey = UUID.randomUUID().toString();
    final var policyWrapKeySetOnContext = new AtomicReference<String>();
    final var retry = RetryPolicy.<Result>builder()
      .policyKey(retryKey)
      .handleResult(Result.FAULT)
      .maxRetryCount(1)
      .build();
    final var breaker = CircuitBreakerPolicy.<Result>builder()
      .policyKey(breakerKey)
      .handleResult(Result.FAULT)
      .durationOfBreak(Duration.ZERO)
      .onBreak(event -> policyWrapKeySetOnContext.set(event.context().getPolicyWrapKey().orElse(null)))
      .build();
    final var wrap = PolicyWrap.wrap(wrapKey, retry, breaker);

    raiseResults(wrap, Result.FAULT, Result.GOOD);

    assertThat(policyWrapKeySetOnContext.get()).isNotEqualTo(retryKey);
    assertThat(policyWrapKeySetOnContext.get()).isNotEqualTo(breakerKey);
    assertThat(policyWrapKeySetOnContext.get()).isEqualTo(wrapKey);
  }

  @Test
  public void shouldRestorePolicyKeyOfOuterPolicyToExecutionContextAsMoveOutwardsThroughPolicyWrap() throws Throwable {
    final var actualFallbackPolicyWrapKey = new AtomicReference<String>();
    final var actualFallbackPolicyKey = new AtomicReference<String>();
    final var fallback = FallbackPolicy.builder(Result.UNDEFINED)
      .policyKey("FallbackPolicy")
      .handle(Exception.class)
      .onFallback((outcome, ctx) -> {
        actualFallbackPolicyWrapKey.set(ctx.getPolicyWrapKey().orElse(null));
        actualFallbackPolicyKey.set(ctx.getPolicyKey().orElse(null));
      })
      .build();
    final var actualRetryPolicyWrapKey = new AtomicReference<String>();
    final var actualRetryPolicyKey = new AtomicReference<String>();
    final var retry = RetryPolicy.<Result>builder()
      .policyKey("RetryPolicy")
      .handle(Exception.class)
      .maxRetryCount(1)
      .onRetry(event -> {
        actualRetryPolicyWrapKey.set(event.context().getPolicyWrapKey().orElse(null));
        actualRetryPolicyKey.set(event.context().getPolicyKey().orElse(null));
      })
      .build();
    final var wrap = PolicyWrap.wrap("PolicyWrap", fallback, retry);

    wrap.execute(() -> {
      throw new Exception();
    });

    assertThat(actualFallbackPolicyWrapKey.get()).isEqualTo("PolicyWrap");
    assertThat(actualFallbackPolicyKey.get()).isEqualTo("FallbackPolicy");
    assertThat(actualRetryPolicyWrapKey.get()).isEqualTo("PolicyWrap");
    assertThat(actualRetryPolicyKey.get()).isEqualTo("RetryPolicy");
  }

  @Test
  public void shouldPassOutermostPolicyWrapKeyAsPolicyWrapKeyIgnoringInnerPolicyWrapKeysEvenWhenExecutingPoliciesInInnerWrap()
    throws Throwable {
    final var retryKey = UUID.randomUUID().toString();
    final var breakerKey = UUID.randomUUID().toString();
    final var fallbackKey = UUID.randomUUID().toString();
    final var innerWrapKey = UUID.randomUUID().toString();
    final var outerWrapKey = UUID.randomUUID().toString();
    final var actualPolicyWrapKey = new AtomicReference<String>();
    final var retry = RetryPolicy.<Result>builder()
      .policyKey(retryKey)
      .handleResult(Result.FAULT)
      .maxRetryCount(1)
      .build();
    final var breaker = CircuitBreakerPolicy.<Result>builder()
      .policyKey(breakerKey)
      .handleResult(Result.FAULT)
      .durationOfBreak(Duration.ZERO)
      .onBreak(event -> actualPolicyWrapKey.set(event.context().getPolicyWrapKey().orElse(null)))
      .build();
    final var fallback = FallbackPolicy.builder(Result.SUBSTITUTE)
      .policyKey(fallbackKey)
      .handleResult(Result.FAULT)
      .build();
    final var innerWrap = PolicyWrap.wrap(innerWrapKey, retry, breaker);
    final var outerWrap = PolicyWrap.wrap(outerWrapKey, fallback, innerWrap);

    raiseResults(outerWrap, Result.FAULT, Result.GOOD);

    assertThat(actualPolicyWrapKey.get()).isNotEqualTo(retryKey);
    assertThat(actualPolicyWrapKey.get()).isNotEqualTo(breakerKey);
    assertThat(actualPolicyWrapKey.get()).isNotEqualTo(fallbackKey);
    assertThat(actualPolicyWrapKey.get()).isNotEqualTo(innerWrapKey);
    assertThat(actualPolicyWrapKey.get()).isEqualTo(outerWrapKey);
  }
}
