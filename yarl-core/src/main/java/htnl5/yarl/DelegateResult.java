package htnl5.yarl;

import htnl5.yarl.functions.CheckedSupplier;

import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import java.util.function.Function;

// producer
public sealed class DelegateResult<R> {
  public boolean isSuccess() {
    return this instanceof DelegateResult.Success<R>;
  }

  public boolean isFailure() {
    return this instanceof DelegateResult.Failure<R>;
  }

  public <T> T fold(final Function<? super R, ? extends T> ifSuccess,
                    final Function<? super Throwable, ? extends T> ifFailure) {
    return switch (this) {
      case Success<R> s -> ifSuccess.apply(s.getResult());
      case Failure<R> f -> ifFailure.apply(f.getException());
      default -> throw new IllegalStateException("Unexpected value: " + this);
    };
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
    return switch (this) {
      case Success<R> s -> s.getResult();
      case Failure<R> f -> throw f.getException();
      default -> throw new IllegalStateException("Unexpected value: " + this);
    };
  }

  public static <R> DelegateResult<R> success(final R result) {
    return new Success<>(result);
  }

  public static <R> DelegateResult<R> failure(final Throwable exception) {
    return new Failure<>(exception);
  }

  public static <R> DelegateResult<R> runCatching(final ExceptionPredicates exceptionPredicates,
                                                  final CheckedSupplier<? extends R> block) {
    try {
      return success(block.get());
    } catch (final CancellationException e) {
      throw e;
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
