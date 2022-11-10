package htnl5.yarl.wrap;

import htnl5.yarl.ExceptionType;
import htnl5.yarl.ISyncPolicy;
import htnl5.yarl.PolicyResult;
import htnl5.yarl.circuitbreaker.CircuitBreakerPolicy;
import htnl5.yarl.circuitbreaker.CircuitBreakerState;
import htnl5.yarl.helpers.Result;
import htnl5.yarl.noop.NoOpPolicy;
import htnl5.yarl.retry.RetryPolicy;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static htnl5.yarl.helpers.PolicyUtils.raiseExceptions;
import static htnl5.yarl.helpers.PolicyUtils.raiseResults;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class PolicyWrapTest {
  //<editor-fold desc="configuration tests">
  @Test
  public void wrappingInnerNullShouldThrow() {
    final var retry = RetryPolicy.<Integer>builder()
      .handleResult(0)
      .maxRetryCount(1)
      .build();

    final var throwable = catchThrowable(() -> PolicyWrap.wrap(retry, null));

    assertThat(throwable).isInstanceOf(NullPointerException.class)
      .hasMessageContaining("Inner policy");
  }

  @Test
  public void wrappingOuterNullShouldThrow() {
    final var retry = RetryPolicy.<Integer>builder()
      .handleResult(0)
      .maxRetryCount(1)
      .build();

    final var throwable = catchThrowable(() -> PolicyWrap.wrap(null, retry));

    assertThat(throwable).isInstanceOf(NullPointerException.class)
      .hasMessageContaining("Outer policy");
  }

  @Test
  public void wrappingVarargFirstNullShouldThrow() {
    final NoOpPolicy<Integer> policy1 = null;
    final var policy2 = NoOpPolicy.<Integer>build();
    final var policy3 = NoOpPolicy.<Integer>build();

    //noinspection ConstantConditions
    final var throwable = catchThrowable(() -> PolicyWrap.wrap(policy1, policy2, policy3));

    assertThat(throwable).isInstanceOf(NullPointerException.class)
      .hasMessageContaining("Outer policy");
  }

  @Test
  public void wrappingVarArgSecondNullShouldThrow() {
    final var policy1 = NoOpPolicy.<Integer>build();
    final NoOpPolicy<Integer> policy2 = null;
    final var policy3 = NoOpPolicy.<Integer>build();

    //noinspection ConstantConditions
    final var throwable = catchThrowable(() -> PolicyWrap.wrap(policy1, policy2, policy3));

    assertThat(throwable).isInstanceOf(NullPointerException.class)
      .hasMessageContaining("Outer policy");
  }

  @Test
  public void wrappingVarArgThirdNullShouldThrow() {
    final var policy1 = NoOpPolicy.<Integer>build();
    final var policy2 = NoOpPolicy.<Integer>build();
    final NoOpPolicy<Integer> policy3 = null;

    //noinspection ConstantConditions
    final var throwable = catchThrowable(() -> PolicyWrap.wrap(policy1, policy2, policy3));

    assertThat(throwable).isInstanceOf(NullPointerException.class)
      .hasMessageContaining("Inner policy");
  }

  @Test
  public void shouldSetOuterInner() {
    final var policyA = NoOpPolicy.<Integer>build();
    final var policyB = NoOpPolicy.<Integer>build();

    final var wrap = PolicyWrap.wrap(policyA, policyB);

    assertThat(wrap.getOuter()).isSameAs(policyA);
    assertThat(wrap.getInner()).isSameAs(policyB);
  }

  @Test
  public void wrappingNothingShouldThrow() {
    final var throwable = catchThrowable(PolicyWrap::<Integer>wrap);

    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Policies");
  }

  @Test
  public void wrappingOnePolicyShouldThrow() {
    final var singlePolicy = RetryPolicy.<Integer>builder()
      .handle(Exception.class)
      .maxRetryCount(1)
      .build();

    //noinspection unchecked
    final var throwable = catchThrowable(() -> PolicyWrap.wrap(new ISyncPolicy[]{singlePolicy}));

    assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Policies");
  }

  @Test
  public void wrappingTwoPoliciesShouldNotThrow() {
    final var retry = RetryPolicy.<Integer>builder()
      .handle(Exception.class)
      .maxRetryCount(1)
      .build();
    final var breaker = CircuitBreakerPolicy
      .<Integer>builder(1, Duration.ofSeconds(10))
      .handle(Exception.class)
      .build();

    //noinspection unchecked
    PolicyWrap.wrap(new ISyncPolicy[]{retry, breaker});
  }
  //</editor-fold>

  //<editor-fold desc="execute tests">
  @Test
  public void wrappingTwoPoliciesAndExecutingShouldWrapOuterThenInnerAroundAction() {
    final var retry = RetryPolicy.<Void>builder()
      .handle(Exception.class)
      .maxRetryCount(1)
      .build();
    final var breaker = CircuitBreakerPolicy
      .<Void>builder(2, Duration.ofMillis(Long.MAX_VALUE))
      .handle(Exception.class)
      .build();
    final var retryWrappingBreaker = PolicyWrap.wrap(retry, breaker);
    final var breakerWrappingRetry = PolicyWrap.wrap(breaker, retry);

    // When the retry wraps the breaker, the retry (being outer) should cause the call to be put through the breaker
    // twice, causing the breaker to break.
    breaker.reset();
    var throwable = catchThrowable(() ->
      raiseExceptions(retryWrappingBreaker, 2, i -> new ArithmeticException()));

    assertThat(throwable).isInstanceOf(ArithmeticException.class);
    assertThat(breaker.getState()).isEqualTo(CircuitBreakerState.OPEN);

    // When the breaker wraps the retry, the retry (being inner) should retry twice before throwing the exception
    // back on the breaker. The exception only hits the breaker once, so the breaker should not break.
    breaker.reset();
    throwable = catchThrowable(() ->
      raiseExceptions(breakerWrappingRetry, 2, i -> new ArithmeticException()));

    assertThat(throwable).isInstanceOf(ArithmeticException.class);
    assertThat(breaker.getState()).isEqualTo(CircuitBreakerState.CLOSED);
  }

  @Test
  public void wrappingTwoResultHandlingPoliciesAndExecutingShouldWrapOuterThenInnerAroundAction() throws Throwable {
    final var retry = RetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .maxRetryCount(1)
      .build();
    final var breaker = CircuitBreakerPolicy
      .<Result>builder(2, Duration.ofMillis(Long.MAX_VALUE))
      .handleResult(Result.FAULT)
      .build();
    final var retryWrappingBreaker = PolicyWrap.wrap(retry, breaker);
    final var breakerWrappingRetry = PolicyWrap.wrap(breaker, retry);

    // When the retry wraps the breaker, the retry (being outer) should cause the call to be put through the breaker
    // twice, causing the breaker to break.
    breaker.reset();
    var result = raiseResults(retryWrappingBreaker, Result.FAULT, Result.FAULT);

    assertThat(result).isEqualTo(Result.FAULT);
    assertThat(breaker.getState()).isEqualTo(CircuitBreakerState.OPEN);

    // When the breaker wraps the retry, the retry (being inner) should retry twice before throwing the exception
    // back on the breaker. The exception only hits the breaker once, so the breaker should not break.
    breaker.reset();
    result = raiseResults(breakerWrappingRetry, Result.FAULT, Result.FAULT);

    assertThat(result).isEqualTo(Result.FAULT);
    assertThat(breaker.getState()).isEqualTo(CircuitBreakerState.CLOSED);
  }
  //</editor-fold>

  //<editor-fold desc="executeAndCapture tests">
  @Test
  public void outermostPolicyHandlingExceptionShouldReportAsPolicyWrapHandledException() {
    final var outerHandlingNPE = CircuitBreakerPolicy
      .<Void>builder(1, Duration.ZERO)
      .handle(NullPointerException.class)
      .build();
    final var innerHandlingAE = CircuitBreakerPolicy
      .<Void>builder(1, Duration.ZERO)
      .handle(ArithmeticException.class)
      .build();
    final var wrap = PolicyWrap.wrap(outerHandlingNPE, innerHandlingAE);

    final var result = wrap.executeAndCapture(() -> {
      throw new NullPointerException();
    });

    assertThat(result).isInstanceOf(PolicyResult.Failure.FailureWithException.class);
    final var f = (PolicyResult.Failure.FailureWithException<Void>) result;
    assertThat(f.getFinalException()).isInstanceOf(NullPointerException.class);
    assertThat(f.getExceptionType()).isEqualTo(ExceptionType.HANDLED_BY_THIS_POLICY);
  }

  @Test
  public void outermostPolicyNotHandlingExceptionEvenIfInnerPoliciesDoShouldReportAsUnhandledException() {
    final var outerHandlingNPE = CircuitBreakerPolicy
      .<Void>builder(1, Duration.ZERO)
      .handle(NullPointerException.class)
      .build();
    final var innerHandlingAE = CircuitBreakerPolicy
      .<Void>builder(1, Duration.ZERO)
      .handle(ArithmeticException.class)
      .build();
    final var wrap = PolicyWrap.wrap(outerHandlingNPE, innerHandlingAE);

    final var result = wrap.executeAndCapture(() -> {
      throw new ArithmeticException();
    });

    assertThat(result).isInstanceOf(PolicyResult.Failure.FailureWithException.class);
    final var f = (PolicyResult.Failure.FailureWithException<Void>) result;
    assertThat(f.getFinalException()).isInstanceOf(ArithmeticException.class);
    assertThat(f.getExceptionType()).isEqualTo(ExceptionType.UNHANDLED);
  }

  @Test
  public void outermostPolicyHandlingResultShouldReportAsPolicyWrapHandledResult() {
    final var outerHandlingFault = CircuitBreakerPolicy
      .<Result>builder(1, Duration.ZERO)
      .handleResult(Result.FAULT)
      .build();
    final var innerHandlingFaultAgain = CircuitBreakerPolicy
      .<Result>builder(1, Duration.ZERO)
      .handleResult(Result.FAULT_AGAIN)
      .build();
    final var wrap = PolicyWrap.wrap(outerHandlingFault, innerHandlingFaultAgain);

    final var result = wrap.executeAndCapture(() -> Result.FAULT);

    assertThat(result).isInstanceOf(PolicyResult.Failure.FailureWithResult.class);
    final var f = (PolicyResult.Failure.FailureWithResult<Result>) result;
    assertThat(f.getFinalHandledResult()).isEqualTo(Result.FAULT);
  }

  @Test
  public void outermostPolicyNotHandlingResultEvenIfInnerPoliciesDoShouldNotReportAsHandled() {
    final var outerHandlingFault = CircuitBreakerPolicy
      .<Result>builder(1, Duration.ZERO)
      .handleResult(Result.FAULT)
      .build();
    final var innerHandlingFaultAgain = CircuitBreakerPolicy
      .<Result>builder(1, Duration.ZERO)
      .handleResult(Result.FAULT_AGAIN)
      .build();
    final var wrap = PolicyWrap.wrap(outerHandlingFault, innerHandlingFaultAgain);

    final var result = wrap.executeAndCapture(() -> Result.FAULT_AGAIN);

    assertThat(result).isInstanceOf(PolicyResult.Success.class);
    final var s = (PolicyResult.Success<Result>) result;
    assertThat(s.getResult()).isEqualTo(Result.FAULT_AGAIN);
  }
  //</editor-fold>
}
