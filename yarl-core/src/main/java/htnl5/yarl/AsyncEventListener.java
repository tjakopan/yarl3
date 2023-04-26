package htnl5.yarl;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

// consumer
public interface AsyncEventListener<E> extends Function<E, CompletableFuture<Void>> {
}
