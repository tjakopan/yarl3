package htnl5.yarl.retry;

import htnl5.yarl.Context;
import htnl5.yarl.DelegateResult;

import java.time.Duration;

// producer
public record RetryEvent<R>(DelegateResult<R> outcome, Duration sleepDuration, int tryCount, Context context) {
}
