package htnl5.yarl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ExceptionPredicates {
  private final List<ExceptionPredicate> predicates = new ArrayList<>();

  public static final ExceptionPredicates NONE = new ExceptionPredicates();

  // internal
  void add(final ExceptionPredicate predicate) {
    predicates.add(predicate);
  }

  public Optional<Throwable> firstMatchOrEmpty(final Throwable e) {
    return predicates.stream()
      .map(p -> p.apply(e))
      .filter(Optional::isPresent)
      .findFirst()
      .orElse(Optional.empty());
  }
}
