package htnl5.yarl;

import java.util.Optional;

// producer of B
public abstract class PolicyBuilderBase<R, B extends PolicyBuilderBase<R, B>> {
  protected String policyKey;
  protected final ResultPredicates<R> resultPredicates = new ResultPredicates<>();
  protected final ExceptionPredicates exceptionPredicates = new ExceptionPredicates();

  public Optional<String> getPolicyKey() {
    return Optional.ofNullable(policyKey);
  }

  public B policyKey(final String policyKey) {
    this.policyKey = policyKey;
    return self();
  }

  public ResultPredicates<R> getResultPredicates() {
    return resultPredicates;
  }

  public ExceptionPredicates getExceptionPredicates() {
    return exceptionPredicates;
  }

  protected abstract B self();
}
