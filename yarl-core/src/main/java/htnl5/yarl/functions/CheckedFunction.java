package htnl5.yarl.functions;

import java.util.Objects;

@FunctionalInterface
public interface CheckedFunction<T, R> {
  R apply(final T t) throws Throwable;

  default <V> CheckedFunction<V, R> compose(final CheckedFunction<? super V, ? extends T> before) {
    Objects.requireNonNull(before);
    return v -> apply(before.apply(v));
  }

  default <V> CheckedFunction<T, V> andThen(final CheckedFunction<? super R, ? extends V> after) {
    Objects.requireNonNull(after);
    return t -> after.apply(apply(t));
  }

  static <T> CheckedFunction<T, T> identity() {
    return t -> t;
  }
}
