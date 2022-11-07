package htnl5.yarl;

public enum ExceptionType {
  HANDLED_BY_THIS_POLICY,
  UNHANDLED;

  static ExceptionType getExceptionType(final ExceptionPredicates exceptionPredicates, final Throwable exception) {
    if (exceptionPredicates.firstMatchOrEmpty(exception).isPresent()) return HANDLED_BY_THIS_POLICY;
    else return UNHANDLED;
  }
}
