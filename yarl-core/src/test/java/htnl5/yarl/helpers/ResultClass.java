package htnl5.yarl.helpers;

public class ResultClass {
  private final Result resultCode;
  private final String someString;

  public ResultClass(final Result resultCode, final String someString) {
    this.resultCode = resultCode;
    this.someString = someString;
  }

  public ResultClass(final Result resultCode) {
    this(resultCode, null);
  }

  public Result getResultCode() {
    return resultCode;
  }

  public String getSomeString() {
    return someString;
  }
}
