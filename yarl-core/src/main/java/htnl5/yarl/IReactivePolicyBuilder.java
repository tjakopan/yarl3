package htnl5.yarl;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface IReactivePolicyBuilder<R, B extends IReactivePolicyBuilder<R, B>> extends IFluentPolicyBuilder<B> {
  ResultPredicates<R> getResultPredicates();

  ExceptionPredicates getExceptionPredicates();

  default B handle(final Class<? extends Throwable> exceptionClass) {
    getExceptionPredicates().add(e -> {
      if (exceptionClass.isInstance(e)) return Optional.of(e);
      else return Optional.empty();
    });
    return self();
  }

  default <E extends Throwable> B handle(final Class<? extends E> exceptionClass,
                                         final Predicate<? super E> exceptionPredicate) {
    getExceptionPredicates().add(e -> {
      //noinspection unchecked
      if (exceptionClass.isInstance(e) && exceptionPredicate.test((E) e)) return Optional.of(e);
      else return Optional.empty();
    });
    return self();
  }

  default B handleCause(final Class<? extends Throwable> causeClass) {
    getExceptionPredicates().add(causePredicate(causeClass::isInstance));
    return self();
  }

  default <E extends Throwable> B handleCause(final Class<? extends E> causeClass,
                                              final Predicate<? super E> exceptionPredicate) {
    //noinspection unchecked
    getExceptionPredicates().add(causePredicate(e -> causeClass.isInstance(e) && exceptionPredicate.test((E) e)));
    return self();
  }

  private ExceptionPredicate causePredicate(final Predicate<? super Throwable> predicate) {
    return e -> Stream.iterate(e, Objects::nonNull, Throwable::getCause)
      .filter(predicate)
      .findFirst();
  }

  default B handleResult(final R result) {
    getResultPredicates().add(r -> r != null && r.equals(result) || r == null && result == null);
    return self();
  }

  default B handleResult(final Predicate<? super R> resultPredicate) {
    getResultPredicates().add(resultPredicate);
    return self();
  }
}
