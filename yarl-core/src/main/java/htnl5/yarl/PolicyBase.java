package htnl5.yarl;

import htnl5.yarl.utilities.KeyHelper;

public abstract class PolicyBase<R, B extends PolicyBuilderBase<R, B>> {
  protected final String policyKey;
  protected final ResultPredicates<R> resultPredicates;
  protected final ExceptionPredicates exceptionPredicates;

  protected PolicyBase(final PolicyBuilderBase<R, B> policyBuilder) {
    policyKey = policyBuilder.getPolicyKey()
      .orElse("%s-%s".formatted(getClass().getSimpleName(), KeyHelper.guidPart()));
    resultPredicates = policyBuilder.getResultPredicates();
    exceptionPredicates = policyBuilder.getExceptionPredicates();
  }

  public String getPolicyKey() {
    return policyKey;
  }

  public ResultPredicates<R> getResultPredicates() {
    return resultPredicates;
  }

  public ExceptionPredicates getExceptionPredicates() {
    return exceptionPredicates;
  }
}
