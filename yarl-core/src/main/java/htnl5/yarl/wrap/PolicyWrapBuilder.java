package htnl5.yarl.wrap;

import htnl5.yarl.*;

import java.util.Arrays;
import java.util.Objects;

public final class PolicyWrapBuilder<R> extends PolicyBuilderBase<R, PolicyWrapBuilder<R>> {
  private ResultPredicates<R> resultPredicates = new ResultPredicates<>();
  private ExceptionPredicates exceptionPredicates = new ExceptionPredicates();

  @Override
  protected PolicyWrapBuilder<R> self() {
    return this;
  }

  public PolicyWrap<R> wrap(final Policy<R, ?> outer, final ISyncPolicy<R> inner) {
    Objects.requireNonNull(outer, "Outer policy must not be null.");
    Objects.requireNonNull(inner, "Inner policy must not be null.");
    this.resultPredicates = outer.getResultPredicates();
    this.exceptionPredicates = outer.getExceptionPredicates();
    return new PolicyWrap<>(this, outer, inner);
  }

  @SafeVarargs
  public final PolicyWrap<R> wrap(final ISyncPolicy<R>... policies) {
    if (policies.length < 2)
      throw new IllegalArgumentException("Policies to form the wrap must contain at least two policies");
    if (policies.length == 2) {
      return wrap((Policy<R, ?>) policies[0], policies[1]);
    } else {
      //noinspection unchecked
      return wrap(policies[0], wrap(Arrays.stream(policies).skip(1).toArray(ISyncPolicy[]::new)));
    }
  }

  @Override
  public ResultPredicates<R> getResultPredicates() {
    return resultPredicates;
  }

  @Override
  public ExceptionPredicates getExceptionPredicates() {
    return exceptionPredicates;
  }
}
