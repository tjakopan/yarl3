package htnl5.yarl.retry;

import java.util.function.Consumer;

// consumer
public interface OnRetryListener<R> extends Consumer<OnRetryEvent<? extends R>> {
}
