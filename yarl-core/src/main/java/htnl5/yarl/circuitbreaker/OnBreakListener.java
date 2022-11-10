package htnl5.yarl.circuitbreaker;

import java.util.function.Consumer;

// consumer
public interface OnBreakListener<R> extends Consumer<OnBreakEvent<? extends R>> {
}
