package htnl5.yarl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class ResultPredicates<R> {
  private final List<Predicate<? super R>> predicates = new ArrayList<>();

  public static final ResultPredicates<?> NONE = new ResultPredicates<>();

  // internal
  void add(final Predicate<? super R> predicate) {
    predicates.add(predicate);
  }

  public boolean anyMatch(final R result) {
    return predicates.stream()
      .anyMatch(p -> p.test(result));
  }
}
