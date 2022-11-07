package htnl5.yarl.functions;

import java.util.Objects;

@FunctionalInterface
public interface Consumer3<T, U, V> {
  void accept(final T t, final U u, final V v);

  default Consumer3<T, U, V> andThen(final Consumer3<? super T, ? super U, ? super V> after) {
    Objects.requireNonNull(after);
    return (t, u, v) -> {
      accept(t, u, v);
      after.accept(t, u, v);
    };
  }
}
