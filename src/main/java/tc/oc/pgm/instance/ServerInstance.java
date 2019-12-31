package tc.oc.pgm.instance;

import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.rotation.Rotation;

public class ServerInstance {

  String name;
  int max;

  boolean locked;
  boolean dynamic;
  String permission;

  Match match;
  Rotation rotation;

  public ServerInstance(String name, int max, boolean locked, boolean dynamic, Rotation rotation) {
    this.name = name;
    this.max = max;
    this.locked = locked;
    this.dynamic = dynamic;
    this.rotation = rotation;

    if (this.locked) {
      permission = "pgm.private." + name.toLowerCase();
    }
  }
}
