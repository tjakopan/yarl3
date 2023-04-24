package htnl5.yarl;

import htnl5.yarl.functions.Consumer3;
import htnl5.yarl.functions.Function3;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

// producer
public sealed class PolicyResult<R> {
  private final Context context;

  public PolicyResult(final Context context) {
    this.context = context;
  }

  public Context getContext() {
    return context;
  }

  public boolean isSuccess() {
    return this instanceof PolicyResult.Success<R>;
  }

  public boolean isFailure() {
    return this instanceof PolicyResult.Failure<R>;
  }

  public boolean isFailureWithException() {
    return this instanceof Failure.FailureWithException<R>;
  }

  public boolean isFailureWithResult() {
    return this instanceof Failure.FailureWithResult<R>;
  }

  public <T> T match(final BiFunction<? super R, Context, ? extends T> success,
                     final Function3<? super Throwable, ExceptionType, Context, ? extends T> failureWithException,
                     final BiFunction<? super R, Context, ? extends T> failureWithResult) {
    if (this instanceof PolicyResult.Success<R> s) {
      return success.apply(s.getResult(), getContext());
    } else if (this instanceof Failure.FailureWithException<R> fe) {
      return failureWithException.apply(fe.getFinalException(), fe.getExceptionType(), getContext());
    } else if (this instanceof Failure.FailureWithResult<R> fr) {
      return failureWithResult.apply(fr.getFinalHandledResult(), getContext());
    } else {
      throw new IllegalStateException("Unexpected value: " + this);
    }
  }

  public PolicyResult<R> onSuccess(final BiConsumer<? super R, Context> action) {
    if (isSuccess()) action.accept(((Success<R>) this).getResult(), getContext());
    return this;
  }

  public PolicyResult<R> onFailure(final Consumer<Context> action) {
    if (isFailure()) action.accept(getContext());
    return this;
  }

  public PolicyResult<R> onFailureWithException(final Consumer3<? super Throwable, ExceptionType, Context> action) {
    if (isFailureWithException()) {
      final var fe = (Failure.FailureWithException<R>) this;
      action.accept(fe.getFinalException(), fe.getExceptionType(), getContext());
    }
    return this;
  }

  public PolicyResult<R> onFailureWithResult(final BiConsumer<? super R, Context> action) {
    if (isFailureWithResult()) {
      final var fr = (Failure.FailureWithResult<R>) this;
      action.accept(fr.getFinalHandledResult(), getContext());
    }
    return this;
  }

  public static <R> PolicyResult<R> success(final R result, final Context context) {
    return new Success<>(result, context);
  }

  public static <R> PolicyResult<R> failureWithException(final Throwable exception,
                                                         final ExceptionType exceptionType, final Context context) {
    return new Failure.FailureWithException<>(exception, exceptionType, context);
  }

  public static <R> PolicyResult<R> failureWithResult(final R handledResult, final Context context) {
    return new Failure.FailureWithResult<>(handledResult, context);
  }

  public static final class Success<R> extends PolicyResult<R> {
    private final R result;

    private Success(final R result, final Context context) {
      super(context);
      this.result = result;
    }

    public R getResult() {
      return result;
    }
  }

  public static sealed class Failure<R> extends PolicyResult<R> {
    private Failure(final Context context) {
      super(context);
    }

    public static final class FailureWithException<R> extends Failure<R> {
      private final Throwable finalException;
      private final ExceptionType exceptionType;

      private FailureWithException(final Throwable finalException, final ExceptionType exceptionType,
                                   final Context context) {
        super(context);
        this.finalException = finalException;
        this.exceptionType = exceptionType;
      }

      public Throwable getFinalException() {
        return finalException;
      }

      public ExceptionType getExceptionType() {
        return exceptionType;
      }
    }

    public static final class FailureWithResult<R> extends Failure<R> {
      private final R finalHandledResult;

      private FailureWithResult(final R finalHandledResult, final Context context) {
        super(context);
        this.finalHandledResult = finalHandledResult;
      }

      public R getFinalHandledResult() {
        return finalHandledResult;
      }
    }
  }
}
