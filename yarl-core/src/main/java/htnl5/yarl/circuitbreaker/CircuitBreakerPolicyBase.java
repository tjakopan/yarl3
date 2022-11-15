package htnl5.yarl.circuitbreaker;

import htnl5.yarl.Context;
import htnl5.yarl.DelegateResult;
import htnl5.yarl.Policy;
import htnl5.yarl.PolicyBuilderBase;
import htnl5.yarl.functions.CheckedFunction;

import java.util.Optional;

public abstract class CircuitBreakerPolicyBase<R, B extends CircuitBreakerPolicyBuilderBase<R, B>>
  extends Policy<R, B> {
  private final ICircuitBreakerController<R> controller;

  protected CircuitBreakerPolicyBase(final PolicyBuilderBase<R, B> policyBuilder,
                                     final ICircuitBreakerController<R> controller) {
    super(policyBuilder);
    this.controller = controller;
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
  protected R implementation(final Context context, final CheckedFunction<Context, ? extends R> action)
    throws Throwable {
    return CircuitBreakerEngine.implementation(action, context, exceptionPredicates, resultPredicates, controller);
  }
}
