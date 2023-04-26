package htnl5.yarl;

import htnl5.yarl.functions.ThrowingSupplier;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.function.Function;

// producer
public sealed class DelegateResult<R> {
  private DelegateResult() {
  }

  public boolean isSuccess() {
    return this instanceof DelegateResult.Success<R>;
  }

  public boolean isFailure() {
    return this instanceof DelegateResult.Failure<R>;
  }

  public <T> T match(final Function<? super R, ? extends T> success,
                     final Function<? super Throwable, ? extends T> failure) {
    if (this instanceof DelegateResult.Success<R> s) {
      return success.apply(s.getResult());
    } else if (this instanceof DelegateResult.Failure<R> f) {
      return failure.apply(f.getException());
    } else {
      throw new IllegalArgumentException("Unexpected value: " + this);
    }
  }

  public CompletableFuture<R> toCompletableFuture() {
    return match(CompletableFuture::completedFuture, CompletableFuture::failedFuture);
  }

  public DelegateResult<R> onSuccess(final Consumer<? super R> action) {
    if (isSuccess()) action.accept(((Success<R>) this).getResult());
    return this;
  }

  public DelegateResult<R> onFailure(final Consumer<? super Throwable> action) {
    if (isFailure()) action.accept(((Failure<R>) this).getException());
    return this;
  }

  public R getOrThrow() throws Throwable {
    if (this instanceof DelegateResult.Success<R> s) {
      return s.getResult();
    } else if (this instanceof DelegateResult.Failure<R> f) {
      throw f.getException();
    } else {
      throw new IllegalStateException("Unexpected value: " + this);
    }
  }

  public static <R> DelegateResult<R> success(final R result) {
    return new Success<>(result);
  }

  public static <R> DelegateResult<R> failure(final Throwable exception) {
    return new Failure<>(exception);
  }

  public static <R> DelegateResult<R> delegateResult(final R result, final Throwable exception) {
    if (exception instanceof CompletionException ce && ce.getCause() != null) {
      return failure(ce.getCause());
    }
    if (exception != null) {
      return failure(exception);
    }
    return success(result);
  }

  public static <R> DelegateResult<R> runCatching(final ExceptionPredicates exceptionPredicates,
                                                  final ThrowingSupplier<? extends R> block) {
    try {
      return success(block.get());
    } catch (final Throwable e) {
      return exceptionPredicates.firstMatchOrEmpty(e)
        .<DelegateResult<R>>map(DelegateResult::failure)
        .orElse(failure(e));
    }
  }

  public static final class Success<R> extends DelegateResult<R> {
    private final R result;

    private Success(final R result) {
      this.result = result;
    }

    public R getResult() {
      return result;
    }
  }

  public static final class Failure<R> extends DelegateResult<R> {
    private final Throwable exception;

    private Failure(final Throwable exception) {
      this.exception = exception;
    }

    public Throwable getException() {
      return exception;
    }
  }
}
