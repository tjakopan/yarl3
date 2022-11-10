package htnl5.yarl.circuitbreaker;

import htnl5.yarl.Context;
import htnl5.yarl.DelegateResult;

import java.time.Duration;

// producer
public record BreakEvent<R>(DelegateResult<R> outcome, CircuitBreakerState state, Duration durationOfBreak,
                            Context context) {
}
