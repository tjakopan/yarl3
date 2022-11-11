package htnl5.yarl.circuitbreaker;

import java.time.Clock;
import java.time.Duration;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

final class RollingHealthMetrics implements IHealthMetrics {
  private final long samplingDuration;
  private final long windowDuration;
  private final Clock clock;
  private final Queue<HealthCount> windows;

  private HealthCount currentWindow;

  private final Lock lock = new ReentrantLock();

  public RollingHealthMetrics(final Duration samplingDuration, final Clock clock, final int numberOfWindows) {
    this.samplingDuration = samplingDuration.toMillis();
    this.windowDuration = this.samplingDuration / numberOfWindows;
    this.clock = clock;
    windows = new LinkedList<>();
  }


  @Override
  public void incrementSuccess() {
    lock.lock();
    try {
      actualiseCurrentMetric();
      currentWindow.setSuccesses(currentWindow.getSuccesses() + 1);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void incrementFailure() {
    lock.lock();
    try {
      actualiseCurrentMetric();
      currentWindow.setFailures(currentWindow.getFailures() + 1);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void reset() {
    lock.lock();
    try {
      currentWindow = null;
      windows.clear();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public HealthCount getHealthCount() {
    lock.lock();
    try {
      actualiseCurrentMetric();
      var successes = 0;
      var failures = 0;
      for (final HealthCount window : windows) {
        successes += window.getSuccesses();
        failures += window.getFailures();
      }
      return new HealthCount(successes, failures, windows.peek().getStartedAt());
    } finally {
      lock.unlock();
    }
  }

  private void actualiseCurrentMetric() {
    lock.lock();
    try {
      final var now = clock.millis();
      if (currentWindow == null || now - currentWindow.getStartedAt() >= windowDuration) {
        currentWindow = new HealthCount(now);
        windows.add(currentWindow);
      }

      while (windows.size() > 0 && (now - windows.peek().getStartedAt() >= samplingDuration)) {
        windows.poll();
      }
    } finally {
      lock.unlock();
    }
  }
}
