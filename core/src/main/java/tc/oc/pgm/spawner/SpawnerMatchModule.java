package tc.oc.pgm.spawner;

import java.util.List;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;

public class SpawnerMatchModule implements MatchModule {

  private Match match;
  private final List<Spawner> spawners;

  public SpawnerMatchModule(Match match, List<Spawner> spawners) {
    this.match = match;
    this.spawners = spawners;
  }

  @Override
  public void load() {
    for (Spawner spawner : this.spawners) {
      match.addListener(spawner, MatchScope.RUNNING);
      match.addTickable(spawner, MatchScope.RUNNING);
      spawner.registerEvents();
    }
  }

  @Override
  public void unload() {
    for (Spawner spawner : this.spawners) {
      spawner.unregisterEvents();
    }
  }
}
