package htnl5.yarl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

// producer of B
public abstract class AsyncPolicyBuilderBase<R, B extends PolicyBuilderBase<R, B>>
  extends PolicyBuilderBase<R, B> {
  protected Executor executor = CompletableFuture.completedFuture(null).defaultExecutor();

  public Executor getExecutor() {
    return executor;
  }

  public B executor(final Executor executor) {
    this.executor = executor;
    return self();
  }
}
