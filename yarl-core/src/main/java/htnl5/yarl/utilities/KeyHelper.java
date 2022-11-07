package htnl5.yarl.utilities;

import java.util.UUID;

public final class KeyHelper {
  private KeyHelper() {
  }

  public static String guidPart() {
    return UUID.randomUUID().toString().substring(0, 8);
  }
}
