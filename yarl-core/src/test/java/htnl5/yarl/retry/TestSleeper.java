package htnl5.yarl.retry;

import java.time.Duration;
import java.util.function.Consumer;

public class TestSleeper implements Sleeper {
  private final Consumer<Duration> onSleep;

  public TestSleeper(final Consumer<Duration> onSleep) {
    this.onSleep = onSleep;
  }

  @Override
  public void sleep(final Duration duration) throws InterruptedException {
    onSleep.accept(duration);
    Thread.sleep(Duration.ZERO);
  }
}
