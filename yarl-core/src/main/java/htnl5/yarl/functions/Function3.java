package htnl5.yarl.functions;

import java.util.Objects;
import java.util.function.Function;

@FunctionalInterface
public interface Function3<T, U, V, R> {
  R apply(T t, U u, V v);

  default <W> Function3<T, U, V, W> andThen(final Function<? super R, ? extends W> after) {
    Objects.requireNonNull(after);
    return (t, u, v) -> after.apply(apply(t, u, v));
  }
}
