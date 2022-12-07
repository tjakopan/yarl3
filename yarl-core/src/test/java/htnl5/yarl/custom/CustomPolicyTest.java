package htnl5.yarl.custom;

import htnl5.yarl.helpers.Result;
import htnl5.yarl.helpers.custom.addbehaviourifhandle.AddBehaviourIfHandlePolicy;
import htnl5.yarl.helpers.custom.preexecute.PreExecutePolicy;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class CustomPolicyTest {
  @Test
  public void shouldBeAbleToConstructActivePolicy() {
    PreExecutePolicy.<Result>builder()
      .preExecute(() -> System.out.println("Do something"))
      .build();
  }

  @Test
  public void activePolicyShouldExecute() throws Throwable {
    final var preExecuted = new AtomicBoolean(false);
    final var policy = PreExecutePolicy.<Result>builder()
      .preExecute(() -> preExecuted.set(true))
      .build();
    final var executed = new AtomicBoolean(false);

    policy.execute(() -> {
      executed.set(true);
      return Result.UNDEFINED;
    });

    assertThat(executed.get()).isTrue();
    assertThat(preExecuted.get()).isTrue();
  }

  @Test
  public void shouldBeAbleToConstructReactivePolicy() {
    AddBehaviourIfHandlePolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .behaviour(outcome -> outcome.onSuccess(r -> System.out.println("Handling " + r)))
      .build();
  }

  @Test
  public void reactivePolicyShouldHandleResult() throws Throwable {
    final var handled = new AtomicReference<Result>();
    final var policy = AddBehaviourIfHandlePolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .behaviour(outcome -> outcome.onSuccess(handled::set))
      .build();
    final var executed = new AtomicBoolean(false);

    final var result = policy.execute(() -> {
      executed.set(true);
      return Result.FAULT;
    });

    assertThat(result).isEqualTo(Result.FAULT);
    assertThat(executed.get()).isTrue();
    assertThat(handled.get()).isEqualTo(Result.FAULT);
  }

  @Test
  public void reactivePolicyShouldHandleException() {
    final var handled = new AtomicReference<Throwable>();
    final var policy = AddBehaviourIfHandlePolicy.builder()
      .handle(IllegalStateException.class)
      .behaviour(outcome -> outcome.onFailure(handled::set))
      .build();
    final var executed = new AtomicBoolean(false);
    final var toThrow = new IllegalStateException();

    final var throwable = catchThrowable(() -> policy.execute(() -> {
      executed.set(true);
      throw toThrow;
    }));

    assertThat(throwable).isInstanceOf(IllegalStateException.class)
      .isEqualTo(toThrow);
    assertThat(executed.get()).isTrue();
    assertThat(handled.get()).isEqualTo(toThrow);
  }

  @Test
  public void reactivePolicyShouldBeAbleToIgnoreUnhandledResult() throws Throwable {
    final var handled = new AtomicReference<Result>();
    final var policy = AddBehaviourIfHandlePolicy.<Result>builder()
      .handleResult(Result.FAULT)
      .behaviour(outcome -> outcome.onSuccess(handled::set))
      .build();
    final var executed = new AtomicBoolean(false);

    final var result = policy.execute(() -> {
      executed.set(true);
      return Result.FAULT_YET_AGAIN;
    });

    assertThat(result).isEqualTo(Result.FAULT_YET_AGAIN);
    assertThat(executed.get()).isTrue();
    assertThat(handled.get()).isNull();
  }

  @Test
  public void reactivePolicyShouldBeAbleToIgnoreUnhandledException() {
    final var handled = new AtomicReference<Throwable>();
    final var policy = AddBehaviourIfHandlePolicy.builder()
      .handle(IllegalStateException.class)
      .behaviour(outcome -> outcome.onFailure(handled::set))
      .build();
    final var executed = new AtomicBoolean(false);
    final var toThrow = new UnsupportedOperationException();

    final var throwable = catchThrowable(() -> policy.execute(() -> {
      executed.set(true);
      throw toThrow;
    }));

    assertThat(throwable).isInstanceOf(UnsupportedOperationException.class)
      .isEqualTo(toThrow);
    assertThat(executed.get()).isTrue();
    assertThat(handled.get()).isNull();
  }
}
