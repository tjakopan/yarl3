package htnl5.yarl.retry;

import htnl5.yarl.Context;
import htnl5.yarl.DelegateResult;

// producer
public record SleepDurationEvent<R>(int tryCount, DelegateResult<R> outcome, Context context) {
}
