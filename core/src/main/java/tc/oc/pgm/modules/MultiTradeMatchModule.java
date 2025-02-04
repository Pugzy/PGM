package tc.oc.pgm.modules;

import static tc.oc.pgm.util.inventory.InventoryUtils.INVENTORY_UTILS;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.events.ListenerScope;

@ListenerScope(MatchScope.LOADED)
public class MultiTradeMatchModule implements MatchModule, Listener {

  private final Logger logger;
  private boolean ok = true;

  public MultiTradeMatchModule(Match match) {
    this.logger = PGM.get().getGameLogger();
  }

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void onInteract(PlayerInteractEntityEvent event) {
    if (event.getRightClicked() instanceof Villager) {
      // Fallback to once-at-a-time trading if multi trade does not work
      if (!ok) return;

      event.setCancelled(true);
      try {
        if (INVENTORY_UTILS.openVillager((Villager) event.getRightClicked(), event.getPlayer()))
          return;

        logger.log(Level.WARNING, "<multitrade/> is not compatible with your server version");
        ok = false;
      } catch (Throwable t) {
        logger.log(
            Level.WARNING,
            String.format(
                "Villager at (%s) has invalid NBT data",
                event.getRightClicked().getLocation().toVector()),
            t);
        ok = false;
      }
    }
  }
}
