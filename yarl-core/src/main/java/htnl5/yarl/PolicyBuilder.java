package htnl5.yarl;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

// producer of B
public abstract class PolicyBuilder<R, B extends PolicyBuilder<R, B>> extends PolicyBuilderBase<R, B> {
  public B handle(final Class<? extends Throwable> exceptionClass) {
    exceptionPredicates.add(e -> {
      if (exceptionClass.isInstance(e)) return Optional.of(e);
      else return Optional.empty();
    });
    return self();
  }

  public <E extends Throwable> B handle(final Class<? extends E> exceptionClass,
                                        final Predicate<? super E> exceptionPredicate) {
    exceptionPredicates.add(e -> {
      //noinspection unchecked
      if (exceptionClass.isInstance(e) && exceptionPredicate.test((E) e)) return Optional.of(e);
      else return Optional.empty();
    });
    return self();
  }

  public B handleCause(final Class<? extends Throwable> causeClass) {
    exceptionPredicates.add(causePredicate(causeClass::isInstance));
    return self();
  }

  public <E extends Throwable> B handleCause(final Class<? extends E> causeClass,
                                             final Predicate<? super E> exceptionPredicate) {
    //noinspection unchecked
    exceptionPredicates.add(causePredicate(e -> causeClass.isInstance(e) && exceptionPredicate.test((E) e)));
    return self();
  }

  ExceptionPredicate causePredicate(final Predicate<Throwable> predicate) {
    return e -> Stream.iterate(e, Objects::nonNull, Throwable::getCause)
      .filter(predicate)
      .findFirst();
  }

  public B handleResult(final R result) {
    resultPredicates.add(r -> r != null && r.equals(result) || r == null && result == null);
    return self();
  }

  public B handleResult(final Predicate<? super R> resultPredicate) {
    resultPredicates.add(resultPredicate);
    return self();
  }

  public abstract IPolicy build();
}
