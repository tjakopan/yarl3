package htnl5.yarl.retry;

interface Sleeper {
  default void sleep(final long millis) throws InterruptedException {
    Thread.sleep(millis);
  }
}
