package htnl5.yarl;

import java.util.Optional;

// producer of B
public abstract class PolicyBuilder<B extends PolicyBuilder<B>> implements IFluentPolicyBuilder<B> {
  protected String policyKey;

  public Optional<String> getPolicyKey() {
    return Optional.ofNullable(policyKey);
  }

  public B policyKey(final String policyKey) {
    this.policyKey = policyKey;
    return self();
  }
}
