package htnl5.yarl.functions;

import java.util.Objects;

@FunctionalInterface
public interface ThrowingFunction<T, R> {
  R apply(final T t) throws Throwable;

  default <V> ThrowingFunction<V, R> compose(final ThrowingFunction<? super V, ? extends T> before) {
    Objects.requireNonNull(before);
    return v -> apply(before.apply(v));
  }

  default <V> ThrowingFunction<T, V> andThen(final ThrowingFunction<? super R, ? extends V> after) {
    Objects.requireNonNull(after);
    return t -> after.apply(apply(t));
  }

  static <T> ThrowingFunction<T, T> identity() {
    return t -> t;
  }
}
