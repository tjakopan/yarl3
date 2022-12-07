package htnl5.yarl.wrap;

import htnl5.yarl.circuitbreaker.CircuitBreakerPolicy;
import htnl5.yarl.circuitbreaker.CircuitBreakerState;
import htnl5.yarl.noop.NoOpPolicy;
import htnl5.yarl.retry.RetryPolicy;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class PolicyWrapGetPoliciesTest {
  @Test
  public void shouldPassAllNestedPoliciesInSameOrderTheyWereAdded() {
    final var policy0 = NoOpPolicy.build();
    final var policy1 = NoOpPolicy.build();
    final var policy2 = NoOpPolicy.build();
    final var policy = PolicyWrap.wrap(policy0, policy1, policy2);

    final var policies = policy.getAllPolicies();

    assertThat(policies.size()).isEqualTo(3);
    assertThat(policies.get(0)).isSameAs(policy0);
    assertThat(policies.get(1)).isSameAs(policy1);
    assertThat(policies.get(2)).isSameAs(policy2);
  }

  @Test
  public void shouldReturnSequenceFromGetPolicies() {
    final var policyA = NoOpPolicy.build();
    final var policyB = NoOpPolicy.build();
    final var policyC = NoOpPolicy.build();
    final var policy = PolicyWrap.wrap(policyA, policyB, policyC);

    final var policies = policy.getAllPolicies();

    assertThat(policies).containsExactly(policyA, policyB, policyC);
  }

  @Test
  public void shouldReturnSequenceFromGetPolicies2() {
    final var policyA = NoOpPolicy.build();
    final var policyB = NoOpPolicy.build();
    final var policyC = NoOpPolicy.build();
    final var policyWrap = PolicyWrap.wrap(policyA, PolicyWrap.wrap(policyB, policyC));

    final var policies = policyWrap.getAllPolicies();

    assertThat(policies).containsExactly(policyA, policyB, policyC);
  }

  @Test
  public void getPoliciesShouldReturnSinglePolicyOfCorrectType() {
    final var policyA = NoOpPolicy.build();
    final var policyB = RetryPolicy.builder()
      .handle(Exception.class)
      .build();
    final var policyC = NoOpPolicy.build();
    final var policyWrap = PolicyWrap.wrap(policyA, policyB, policyC);

    final var policies = policyWrap.getPolicies(RetryPolicy.class);

    assertThat(policies).containsExactly(policyB);
  }

  @Test
  public void getPoliciesShouldReturnEmptyListIfNoPolicyOfThatType() {
    final var policyA = NoOpPolicy.build();
    final var policyB = RetryPolicy.builder()
      .handle(Exception.class)
      .build();
    final var policyC = NoOpPolicy.build();
    final var policyWrap = PolicyWrap.wrap(policyA, policyB, policyC);

    final var policies = policyWrap.getPolicies(CircuitBreakerPolicy.class);

    assertThat(policies).isEmpty();
  }

  @Test
  public void getPoliciesShouldReturnMultiplePoliciesOfTheType() {
    final var policyA = NoOpPolicy.build();
    final var policyB = RetryPolicy.builder()
      .handle(Exception.class)
      .build();
    final var policyC = NoOpPolicy.build();
    final var policyWrap = PolicyWrap.wrap(policyA, policyB, policyC);

    final var policies = policyWrap.getPolicies(NoOpPolicy.class);

    assertThat(policies).containsExactly(policyA, policyC);
  }

  @Test
  public void getPoliciesShouldReturnPoliciesOfTheTypeMatchingPredicate() {
    final var policyA = CircuitBreakerPolicy.builder()
      .handle(Exception.class)
      .durationOfBreak(Duration.ZERO)
      .build();
    final var policyB = RetryPolicy.builder()
      .handle(Exception.class)
      .build();
    final var policyC = CircuitBreakerPolicy.builder()
      .handle(Exception.class)
      .durationOfBreak(Duration.ZERO)
      .build();
    policyA.isolate();
    final var policyWrap = PolicyWrap.wrap(policyA, policyB, policyC);

    final var policies =
      policyWrap.getPolicies(CircuitBreakerPolicy.class, p -> p.getState() == CircuitBreakerState.CLOSED);

    assertThat(policies).containsExactly(policyC);
  }

  @Test
  public void getPoliciesShouldReturnEmptyListIfNoneMatchPredicate() {
    final var policyA = CircuitBreakerPolicy.builder()
      .handle(Exception.class)
      .durationOfBreak(Duration.ZERO)
      .build();
    final var policyB = RetryPolicy.builder()
      .handle(Exception.class)
      .build();
    final var policyC = CircuitBreakerPolicy.builder()
      .handle(Exception.class)
      .durationOfBreak(Duration.ZERO)
      .build();
    final var policyWrap = PolicyWrap.wrap(policyA, policyB, policyC);

    final var policies =
      policyWrap.getPolicies(CircuitBreakerPolicy.class, p -> p.getState() == CircuitBreakerState.OPEN);

    assertThat(policies).isEmpty();
  }

  @Test
  public void getPoliciesWithPredicateShouldReturnMultiplePoliciesOfTheTypeIfMultipleMatchPredicate() {
    final var policyA = NoOpPolicy.build();
    final var policyB = RetryPolicy.builder()
      .handle(Exception.class)
      .build();
    final var policyC = NoOpPolicy.build();
    final var policyWrap = PolicyWrap.wrap(policyA, policyB, policyC);

    final var policies = policyWrap.getPolicies(NoOpPolicy.class, p -> true);

    assertThat(policies).containsExactly(policyA, policyC);
  }

  @Test
  public void getPoliciesShouldThrowIfClassIsNull() {
    final var policyA = NoOpPolicy.build();
    final var policyB = NoOpPolicy.build();
    final var policyWrap = PolicyWrap.wrap(policyA, policyB);

    final var throwable = catchThrowable(() -> policyWrap.getPolicies(null));

    assertThat(throwable).isInstanceOf(NullPointerException.class)
      .hasMessageContaining("policyClass");
  }

  @Test
  public void getPoliciesWithPredicateShouldThrowIfClassIsNull() {
    final var policyA = NoOpPolicy.build();
    final var policyB = NoOpPolicy.build();
    final var policyWrap = PolicyWrap.wrap(policyA, policyB);

    final var throwable = catchThrowable(() -> policyWrap.getPolicies(null, p -> true));

    assertThat(throwable).isInstanceOf(NullPointerException.class)
      .hasMessageContaining("policyClass");
  }

  @Test
  public void getPoliciesWithPredicateShouldThrowIfPredicateIsNull() {
    final var policyA = NoOpPolicy.build();
    final var policyB = NoOpPolicy.build();
    final var policyWrap = PolicyWrap.wrap(policyA, policyB);

    final var throwable = catchThrowable(() -> policyWrap.getPolicies(NoOpPolicy.class, null));

    assertThat(throwable).isInstanceOf(NullPointerException.class)
      .hasMessageContaining("predicate");
  }

  @Test
  public void getPolicyShouldReturnSinglePolicyOfTheType() {
    final var policyA = NoOpPolicy.build();
    final var policyB = RetryPolicy.builder()
      .handle(Exception.class)
      .build();
    final var policyC = NoOpPolicy.build();
    final var policyWrap = PolicyWrap.wrap(policyA, policyB, policyC);

    final var policy = policyWrap.getPolicy(RetryPolicy.class).orElse(null);

    assertThat(policy).isSameAs(policyB);
  }

  @Test
  public void getPolicyShouldReturnEmptyOptionalIfNoPolicyOfTheType() {
    final var policyA = NoOpPolicy.build();
    final var policyB = RetryPolicy.builder()
      .handle(Exception.class)
      .build();
    final var policyC = NoOpPolicy.build();
    final var policyWrap = PolicyWrap.wrap(policyA, policyB, policyC);

    final var policy = policyWrap.getPolicy(CircuitBreakerPolicy.class);

    assertThat(policy).isEmpty();
  }

  @Test
  public void getPolicyShouldThrowIfMultiplePoliciesOfTheType() {
    final var policyA = NoOpPolicy.build();
    final var policyB = RetryPolicy.builder()
      .handle(Exception.class)
      .build();
    final var policyC = NoOpPolicy.build();
    final var policyWrap = PolicyWrap.wrap(policyA, policyB, policyC);

    final var throwable = catchThrowable(() -> policyWrap.getPolicy(NoOpPolicy.class));

    assertThat(throwable).isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void getPolicyShouldReturnSinglePolicyOfTheTypeMatchingPredicate() {
    final var policyA = CircuitBreakerPolicy.builder()
      .handle(Exception.class)
      .durationOfBreak(Duration.ZERO)
      .build();
    final var policyB = RetryPolicy.builder()
      .handle(Exception.class)
      .build();
    final var policyC = CircuitBreakerPolicy.builder()
      .handle(Exception.class)
      .durationOfBreak(Duration.ZERO)
      .build();
    policyA.isolate();
    final var policyWrap = PolicyWrap.wrap(policyA, policyB, policyC);

    final var policy =
      policyWrap.getPolicy(CircuitBreakerPolicy.class, p -> p.getState() == CircuitBreakerState.CLOSED).orElse(null);

    assertThat(policy).isSameAs(policyC);
  }

  @Test
  public void getPolicyShouldReturnEmptyOptionalIfNoneMatchPredicate() {
    final var policyA = CircuitBreakerPolicy.builder()
      .handle(Exception.class)
      .durationOfBreak(Duration.ZERO)
      .build();
    final var policyB = RetryPolicy.builder()
      .handle(Exception.class)
      .build();
    final var policyC = CircuitBreakerPolicy.builder()
      .handle(Exception.class)
      .durationOfBreak(Duration.ZERO)
      .build();
    final var policyWrap = PolicyWrap.wrap(policyA, policyB, policyC);

    final var policy =
      policyWrap.getPolicy(CircuitBreakerPolicy.class, p -> p.getState() == CircuitBreakerState.OPEN);

    assertThat(policy).isEmpty();
  }

  @Test
  public void getPolicyWithPredicateShouldThrowIfMultiplePoliciesMatchPredicate() {
    final var policyA = NoOpPolicy.build();
    final var policyB = RetryPolicy.builder()
      .handle(Exception.class)
      .build();
    final var policyC = NoOpPolicy.build();
    final var policyWrap = PolicyWrap.wrap(policyA, policyB, policyC);

    final var throwable = catchThrowable(() -> policyWrap.getPolicy(NoOpPolicy.class, p -> true));

    assertThat(throwable).isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void getPolicyShouldThrowIfClassIsNull() {
    final var policyA = NoOpPolicy.build();
    final var policyB = NoOpPolicy.build();
    final var policyWrap = PolicyWrap.wrap(policyA, policyB);

    final var throwable = catchThrowable(() -> policyWrap.getPolicy(null));

    assertThat(throwable).isInstanceOf(NullPointerException.class)
      .hasMessageContaining("policyClass");
  }

  @Test
  public void getPolicyWithPredicateShouldThrowIfClassIsNull() {
    final var policyA = NoOpPolicy.build();
    final var policyB = NoOpPolicy.build();
    final var policyWrap = PolicyWrap.wrap(policyA, policyB);

    final var throwable = catchThrowable(() -> policyWrap.getPolicy(null, p -> true));

    assertThat(throwable).isInstanceOf(NullPointerException.class)
      .hasMessageContaining("policyClass");
  }

  @Test
  public void getPolicyWithPredicateShouldThrowIfPredicateIsNull() {
    final var policyA = NoOpPolicy.build();
    final var policyB = NoOpPolicy.build();
    final var policyWrap = PolicyWrap.wrap(policyA, policyB);

    final var throwable = catchThrowable(() -> policyWrap.getPolicy(NoOpPolicy.class, null));

    assertThat(throwable).isInstanceOf(NullPointerException.class)
      .hasMessageContaining("predicate");
  }
}
