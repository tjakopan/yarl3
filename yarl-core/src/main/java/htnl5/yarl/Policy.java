package htnl5.yarl;

import htnl5.yarl.utilities.KeyHelper;

public abstract class Policy<B extends PolicyBuilder<B>> {
  protected final String policyKey;

  protected Policy(final B policyBuilder) {
    policyKey = policyBuilder.getPolicyKey()
      .orElse("%s-%s".formatted(getClass().getSimpleName(), KeyHelper.guidPart()));
  }

  public String getPolicyKey() {
    return policyKey;
  }
}
