package tc.oc.pgm.portals;

import java.util.Set;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import tc.oc.pgm.api.event.CoarsePlayerMoveEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.filters.FilterMatchModule;

@ListenerScope(MatchScope.LOADED)
public class PortalMatchModule implements MatchModule, Listener {

  private final Match match;
  protected final Set<Portal> portals;

  public PortalMatchModule(Match match, Set<Portal> portals) {
    this.match = match;
    this.portals = portals;
  }

  @Override
  public void load() throws ModuleLoadException {

    FilterMatchModule fmm = match.needModule(FilterMatchModule.class);

    portals.forEach(portal -> portal.load(fmm));
  }
}
