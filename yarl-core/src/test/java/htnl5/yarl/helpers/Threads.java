package htnl5.yarl.helpers;

import java.time.Duration;

public final class Threads {
  private Threads() {
  }

  public static boolean join(final Thread thread, final Duration duration) throws InterruptedException {
    thread.join(duration.toMillis());
    return thread.getState() == Thread.State.TERMINATED;
  }
}
