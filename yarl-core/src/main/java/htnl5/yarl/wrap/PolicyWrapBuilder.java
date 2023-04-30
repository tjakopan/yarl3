package htnl5.yarl.wrap;

import htnl5.yarl.ISyncPolicy;
import htnl5.yarl.PolicyBuilder;

import java.util.Arrays;
import java.util.Objects;

final class PolicyWrapBuilder<R> extends PolicyBuilder<PolicyWrapBuilder<R>> {
  @Override
  public PolicyWrapBuilder<R> self() {
    return this;
  }

  PolicyWrap<R> wrap(final ISyncPolicy<R> outer, final ISyncPolicy<R> inner) {
    Objects.requireNonNull(outer, "Outer policy must not be null.");
    Objects.requireNonNull(inner, "Inner policy must not be null.");
    return new PolicyWrap<>(this, outer, inner);
  }

  @SafeVarargs
  final PolicyWrap<R> wrap(final ISyncPolicy<R>... policies) {
    if (policies.length < 2)
      throw new IllegalArgumentException("Policies to form the wrap must contain at least two policies");
    if (policies.length == 2) {
      return wrap(policies[0], policies[1]);
    } else {
      //noinspection unchecked
      return wrap(policies[0], wrap(Arrays.stream(policies).skip(1).toArray(ISyncPolicy[]::new)));
    }
  }
}
