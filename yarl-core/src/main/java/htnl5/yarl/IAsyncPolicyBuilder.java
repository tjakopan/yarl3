package htnl5.yarl;

import java.util.concurrent.Executor;

public interface IAsyncPolicyBuilder<B extends IAsyncPolicyBuilder<B>> extends IFluentPolicyBuilder<B> {
  Executor getExecutor();

  B executor(final Executor executor);
}
