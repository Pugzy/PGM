package tc.oc.pgm.portals;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.Tickable;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.filters.FilterMatchModule;

import java.util.ArrayList;
import java.util.Set;

@ListenerScope(MatchScope.LOADED)
public class PortalMatchModule implements MatchModule, Listener, Tickable {

  private final Match match;
  protected final Set<Portal> portals;

  final static ArrayList<MatchPlayer> teleportedPlayers = new ArrayList<>();

  public PortalMatchModule(Match match, Set<Portal> portals) {
    this.match = match;
    this.portals = portals;
    match.addTickable(this, MatchScope.LOADED);
  }

  @Override
  public void load() throws ModuleLoadException {
    FilterMatchModule fmm = match.needModule(FilterMatchModule.class);

    portals.forEach(portal -> portal.load(fmm));
  }

  public static boolean teleported(MatchPlayer player) {
    if (teleportedPlayers.contains(player)) {
        return false;
    } else {
      teleportedPlayers.add(player);
      return true;
    }
  }

  @Override
  public void tick(Match match, Tick tick) {
    teleportedPlayers.clear();
  }
}
