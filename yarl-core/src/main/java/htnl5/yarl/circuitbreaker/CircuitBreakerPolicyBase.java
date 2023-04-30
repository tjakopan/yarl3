package htnl5.yarl.circuitbreaker;

import htnl5.yarl.*;
import htnl5.yarl.functions.ThrowingFunction;

import java.util.Optional;

public abstract class CircuitBreakerPolicyBase<R, B extends CircuitBreakerPolicyBuilderBase<R, B>>
  extends Policy<B>
  implements IReactiveSyncPolicy<R> {
  private final ResultPredicates<R> resultPredicates;
  private final ExceptionPredicates exceptionPredicates;
  private final ICircuitBreakerController<R> controller;

  protected CircuitBreakerPolicyBase(final B policyBuilder, final ICircuitBreakerController<R> controller) {
    super(policyBuilder);
    this.resultPredicates = policyBuilder.getResultPredicates();
    this.exceptionPredicates = policyBuilder.getExceptionPredicates();
    this.controller = controller;
  }

  @Override
  public ResultPredicates<R> getResultPredicates() {
    return resultPredicates;
  }

  @Override
  public ExceptionPredicates getExceptionPredicates() {
    return exceptionPredicates;
  }

  ICircuitBreakerController<R> getController() {
    return controller;
  }

  public CircuitBreakerState getState() {
    return controller.getState();
  }

  public Optional<DelegateResult<R>> getLastOutcome() {
    return controller.getLastOutcome();
  }

  public void isolate() {
    controller.isolate();
  }

  public void reset() {
    controller.reset();
  }

  @Override
  public R implementation(final Context context, final ThrowingFunction<Context, ? extends R> action)
    throws Throwable {
    return CircuitBreakerEngine.implementation(action, context, exceptionPredicates, resultPredicates, controller);
  }
}
