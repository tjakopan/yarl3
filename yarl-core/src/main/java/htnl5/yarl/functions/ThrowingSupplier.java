package htnl5.yarl.functions;

@FunctionalInterface
public interface ThrowingSupplier<T> {
  T get() throws Throwable;
}
