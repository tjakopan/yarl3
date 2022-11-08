package htnl5.yarl.retry;

import java.time.Duration;

interface Sleeper {
  default void sleep(final Duration duration) throws InterruptedException {
    Thread.sleep(duration);
  }
}
