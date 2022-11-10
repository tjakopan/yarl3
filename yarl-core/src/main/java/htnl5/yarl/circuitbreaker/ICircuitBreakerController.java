package htnl5.yarl.circuitbreaker;

import htnl5.yarl.Context;
import htnl5.yarl.DelegateResult;

import java.util.Optional;

interface ICircuitBreakerController<R> {
  CircuitBreakerState getState();

  Optional<DelegateResult<R>> getLastOutcome();

  void isolate();

  void reset();

  void onActionPreExecute() throws BrokenCircuitException;

  void onActionSuccess(final Context context);

  void onActionFailure(final DelegateResult<? extends R> outcome, final Context context);
}
