package htnl5.yarl.helpers;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

public class MutableClock extends Clock {
  private volatile Instant instant;
  private final ZoneId zone;

  public MutableClock(final Instant initialInstant, ZoneId zone) {
    instant = initialInstant;
    this.zone = zone;
  }

  @Override
  public ZoneId getZone() {
    return zone;
  }

  @Override
  public Clock withZone(final ZoneId zone) {
    if (zone.equals(this.zone)) {
      return this;
    }
    return new MutableClock(instant, zone);
  }

  @Override
  public Instant instant() {
    return instant;
  }

  public MutableClock setInstant(final Instant instant) {
    this.instant = instant;
    return this;
  }

  @Override
  public boolean equals(final Object obj) {
    return obj instanceof MutableClock other && instant.equals(other.instant) && zone.equals(other.zone);
  }

  @Override
  public int hashCode() {
    return instant.hashCode() ^ zone.hashCode();
  }

  @Override
  public String toString() {
    return "MutableClock[" + instant + "," + zone + "]";
  }
}
