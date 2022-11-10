package htnl5.yarl.circuitbreaker;

import htnl5.yarl.Context;
import htnl5.yarl.DelegateResult;
import htnl5.yarl.Policy;
import htnl5.yarl.functions.CheckedFunction;

import java.util.Optional;

public final class CircuitBreakerPolicy<R> extends Policy<R, CircuitBreakerPolicyBuilder<R>>
  implements ICircuitBreakerPolicy {
  private final ICircuitBreakerController<R> controller;

  CircuitBreakerPolicy(final CircuitBreakerPolicyBuilder<R> policyBuilder,
                       final ICircuitBreakerController<R> controller) {
    super(policyBuilder);
    this.controller = controller;
  }

  public static <R> CircuitBreakerPolicyBuilder<R> builder() {
    return new CircuitBreakerPolicyBuilder<>();
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
