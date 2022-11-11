package htnl5.yarl.circuitbreaker;

class HealthCount {
  private int successes;
  private int failures;
  private long startedAt;

  public HealthCount(final int successes, final int failures, final long startedAt) {
    this.successes = successes;
    this.failures = failures;
    this.startedAt = startedAt;
  }

  public HealthCount(final long startedAt) {
    this.startedAt = startedAt;
  }

  public int getSuccesses() {
    return successes;
  }

  public HealthCount setSuccesses(final int successes) {
    this.successes = successes;
    return this;
  }

  public int getFailures() {
    return failures;
  }

  public HealthCount setFailures(final int failures) {
    this.failures = failures;
    return this;
  }

  public int getTotal() {
    return successes + failures;
  }

  public long getStartedAt() {
    return startedAt;
  }

  public HealthCount setStartedAt(final long startedAt) {
    this.startedAt = startedAt;
    return this;
  }
}
