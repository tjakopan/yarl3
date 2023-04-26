package htnl5.yarl.retry;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

interface SleepExecutorProvider {
  default Executor sleepExecutor(final Duration duration, final Executor baseExecutor) {
    return CompletableFuture.delayedExecutor(duration.toMillis(), TimeUnit.MILLISECONDS, baseExecutor);
  }
}
