package htnl5.yarl.circuitbreaker;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

final class SingleHealthMetrics implements IHealthMetrics {
  private final long samplingDuration;
  private final Clock clock;

  private HealthCount current;

  private final Lock lock = new ReentrantLock();

  public SingleHealthMetrics(final Duration samplingDuration, final Clock clock) {
    this.samplingDuration = samplingDuration.toMillis();
    this.clock = clock;
  }

  @Override
  public void incrementSuccess() {
    lock.lock();
    try {
      actualiseCurrentMetric();
      current.setSuccesses(current.getSuccesses() + 1);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void incrementFailure() {
    lock.lock();
    try {
      actualiseCurrentMetric();
      current.setFailures(current.getFailures() + 1);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void reset() {
    lock.lock();
    try {
      current = null;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public HealthCount getHealthCount() {
    lock.lock();
    try {
      actualiseCurrentMetric();
      return current;
    } finally {
      lock.unlock();
    }
  }

  private void actualiseCurrentMetric() {
    lock.lock();
    try {
      final var now = clock.millis();
      if (current == null || now - current.getStartedAt() >= samplingDuration) {
        current = new HealthCount(now);
      }
    } finally {
      lock.unlock();
    }
  }
}
