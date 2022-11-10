package htnl5.yarl;

public abstract class ExecutionRejectedException extends Exception {
  protected ExecutionRejectedException() {
  }

  protected ExecutionRejectedException(final String message) {
    super(message);
  }

  protected ExecutionRejectedException(final String message, final Throwable cause) {
    super(message, cause);
  }

  protected ExecutionRejectedException(final Throwable cause) {
    super(cause);
  }

  protected ExecutionRejectedException(final String message, final Throwable cause, final boolean enableSuppression,
                                       final boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
