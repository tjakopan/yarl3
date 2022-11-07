package htnl5.yarl;

import java.util.Optional;
import java.util.function.Function;

@FunctionalInterface
public interface ExceptionPredicate extends Function<Throwable, Optional<Throwable>> {
}
