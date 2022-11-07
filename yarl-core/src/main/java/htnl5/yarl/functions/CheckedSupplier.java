package htnl5.yarl.functions;

@FunctionalInterface
public interface CheckedSupplier<T> {
  T get() throws Throwable;
}
