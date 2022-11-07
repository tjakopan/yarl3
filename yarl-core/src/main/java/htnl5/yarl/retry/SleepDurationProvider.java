package htnl5.yarl.retry;

import java.time.Duration;
import java.util.function.Function;

// consumer
public interface SleepDurationProvider<R> extends Function<SleepDurationEvent<? extends R>, Duration> {
}
