package htnl5.yarl;

import htnl5.yarl.helpers.Result;
import htnl5.yarl.noop.NoOpPolicy;
import htnl5.yarl.retry.RetryPolicy;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class PolicyTest {
  //<editor-fold desc="execute tests">
  @Test
  public void executingThePolicyActionShouldExecuteTheSpecifiedActionAndReturnTheResult() throws Throwable {
    final var policy = RetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .maxRetryCount(1)
      .build();

    final var result = policy.execute(() -> Result.GOOD);

    assertThat(result).isEqualTo(Result.GOOD);
  }
  //</editor-fold>

  //<editor-fold desc="executeAndCapture tests">
  @Test
  public void executingAndCapturingThePolicyActionSuccessfullyShouldReturnSuccessResult() {
    final var result = RetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .maxRetryCount(1)
      .build()
      .executeAndCapture(() -> Result.GOOD);

    assertThat(result.isSuccess()).isTrue();
    final var success = (PolicyResult.Success<Result>) result;
    assertThat(success.getResult()).isEqualTo(Result.GOOD);
  }

  @Test
  public void executingAndCapturingThePolicyActionAndFailingWithAHandledExceptionTypeShouldReturnFailureResultIndicatingThatExceptionTypeIsOneHandledByThisPolicy() {
    final var handledException = new ArithmeticException();

    final var result = RetryPolicy.<Void>builder()
      .handle(ArithmeticException.class)
      .maxRetryCount(1)
      .build()
      .executeAndCapture(() -> {
        throw handledException;
      });

    assertThat(result.isFailureWithException()).isTrue();
    final var failure = (PolicyResult.Failure.FailureWithException<Void>) result;
    assertThat(failure.getFinalException()).isSameAs(handledException);
    assertThat(failure.getExceptionType()).isEqualTo(ExceptionType.HANDLED_BY_THIS_POLICY);
  }

  @Test
  public void executingAndCapturingThePolicyActionAndFailingWithAnUnhandledExceptionTypeShouldReturnFailureResultIndicatingThatExceptionTypeIsNotHandledByThisPolicy() {
    final var unhandledException = new Exception();

    final var result = RetryPolicy.<Void>builder()
      .handle(ArithmeticException.class)
      .maxRetryCount(1)
      .build()
      .executeAndCapture(() -> {
        throw unhandledException;
      });

    assertThat(result.isFailureWithException()).isTrue();
    final var failure = (PolicyResult.Failure.FailureWithException<Void>) result;
    assertThat(failure.getFinalException()).isSameAs(unhandledException);
    assertThat(failure.getExceptionType()).isEqualTo(ExceptionType.UNHANDLED);
  }

  @Test
  public void executingAndCapturingThePolicyActionAndFailingWithAHandledResultShouldReturnFailureResultIndicatingThatResultIsOneHandledByThisPolicy() {
    final var handledResult = Result.FAULT;

    final var result = RetryPolicy.<Result>builder()
      .handleResult(handledResult)
      .maxRetryCount(1)
      .build()
      .executeAndCapture(() -> handledResult);

    assertThat(result.isFailureWithResult()).isTrue();
    final var failure = (PolicyResult.Failure.FailureWithResult<Result>) result;
    assertThat(failure.getFinalHandledResult()).isSameAs(handledResult);
  }
  //</editor-fold>

  //<editor-fold desc="context tests">
  @Test
  public void executingThePolicyActionShouldThrowWhenContextDataIsNull() {
    final var policy = RetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .maxRetryCount(1)
      .build();

    final var throwable = catchThrowable(() -> policy.execute((Map<String, Object>) null, ctx -> Result.GOOD));

    assertThat(throwable).isInstanceOf(NullPointerException.class)
      .hasMessageContaining("contextData");
  }

  @Test
  public void executingThePolicyActionShouldThrowWhenContextIsNull() {
    final var policy = RetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .maxRetryCount(1)
      .build();

    final var throwable = catchThrowable(() -> policy.execute(null, ctx -> Result.GOOD));

    assertThat(throwable).isInstanceOf(NullPointerException.class)
      .hasMessageContaining("context");
  }

  @Test
  public void executingThePolicyActionShouldPassContextToExecutedAction() throws Throwable {
    final var operationKey = "SomeKey";
    final var executionContext = new Context(operationKey);
    final var capturedContext = new AtomicReference<Context>();
    final var policy = NoOpPolicy.<Result>build();

    policy.execute(executionContext, ctx -> {
      capturedContext.set(ctx);
      return Result.GOOD;
    });

    assertThat(capturedContext.get()).isSameAs(executionContext);
  }

  @Test
  public void executingAndCapturingThePolicyActionShouldThrowWhenContextDataIsNull() {
    final var policy = RetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .build();

    final var throwable =
      catchThrowable(() -> policy.executeAndCapture((Map<String, Object>) null, ctx -> Result.GOOD));

    assertThat(throwable).isInstanceOf(NullPointerException.class)
      .hasMessageContaining("contextData");
  }

  @Test
  public void executingAndCapturingThePolicyActionShouldThrowWhenContextIsNull() {
    final var policy = RetryPolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .build();

    final var throwable = catchThrowable(() -> policy.executeAndCapture(null, ctx -> Result.GOOD));

    assertThat(throwable).isInstanceOf(NullPointerException.class)
      .hasMessageContaining("context");
  }

  @Test
  public void executingAndCapturingThePolicyActionShouldPassContextToExecutedAction() {
    final var operationKey = "SomeKey";
    final var executionContext = new Context(operationKey);
    final var capturedContext = new AtomicReference<Context>();
    final var policy = NoOpPolicy.<Result>build();

    policy.executeAndCapture(executionContext, ctx -> {
      capturedContext.set(ctx);
      return Result.GOOD;
    });

    assertThat(capturedContext.get()).isSameAs(executionContext);
  }

  @Test
  public void executingAndCapturingThePolicyActionShouldPassContextToPolicyResult() {
    final var operationKey = "SomeKey";
    final var executionContext = new Context(operationKey);
    final var policy = NoOpPolicy.<Result>build();

    final var policyResult = policy.executeAndCapture(executionContext, ctx -> Result.GOOD);

    assertThat(policyResult.getContext()).isSameAs(executionContext);
  }
  //</editor-fold>
}
