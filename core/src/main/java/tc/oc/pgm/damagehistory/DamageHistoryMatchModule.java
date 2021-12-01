package tc.oc.pgm.damagehistory;

import java.util.Deque;
import javax.annotation.Nullable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PotionEffectAddEvent;
import org.bukkit.potion.PotionEffectType;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.api.tracker.info.DamageInfo;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.util.nms.NMSHacks;

@ListenerScope(MatchScope.RUNNING)
public class DamageHistoryMatchModule implements MatchModule, Listener {

  private final Match match;
  private final HistoricTracker historicTracker;

  public DamageHistoryMatchModule(Match match) {
    this.match = match;
    this.historicTracker = new HistoricTracker();
  }

  TrackerMatchModule tracker() {
    return match.needModule(TrackerMatchModule.class);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onDamage(EntityDamageEvent event) {
    MatchPlayer victim = getVictim(event.getEntity());
    if (victim == null) return;

    DamageInfo damageInfo = tracker().resolveDamage(event);

    // Store damage with value
    MatchPlayer attacker =
        damageInfo.getAttacker() != null ? damageInfo.getAttacker().getPlayer().orElse(null) : null;

    historicTracker.addDamage(victim, getDamageAmount(event), attacker);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(final MatchPlayerDeathEvent event) {

    Deque<HistoricDamage> playerHistory =
        historicTracker.getPlayerHistory(event.getPlayer().getId());

    event
        .getPlayer()
        .getMatch()
        .sendMessage(historicTracker.broadcast(event.getPlayer(), playerHistory));

    playerHistory.clear();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onDamageMonitor(final EntityRegainHealthEvent event) {
    MatchPlayer victim = getVictim(event.getEntity());
    if (victim == null) return;

    // Bukkit.broadcastMessage(event.getRegainReason().toString());

    double maxHealing = victim.getBukkit().getMaxHealth() - victim.getBukkit().getHealth();

    historicTracker.removeDamage(victim, Math.min(maxHealing, event.getAmount()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPotionEffectAdd(final PotionEffectAddEvent event) {
    if (!event.getEffect().getType().equals(PotionEffectType.ABSORPTION)) return;

    MatchPlayer victim = getVictim(event.getEntity());
    if (victim == null) return;

    double currentHearts = NMSHacks.getAbsorption(event.getEntity());
    double newHearts = (event.getEffect().getAmplifier() + 1) * 4;

    historicTracker.removeDamage(victim, Math.max(0, newHearts - currentHearts));
  }

  @Nullable
  MatchPlayer getVictim(Entity entity) {
    if (entity == null) return null;

    return match.getParticipant(entity);
  }

  private double getDamageAmount(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player)) return 0;

    double absorptionHearts = -event.getDamage(EntityDamageEvent.DamageModifier.ABSORPTION);
    double realFinalDamage =
        Math.min(event.getFinalDamage(), ((Player) event.getEntity()).getHealth())
            + absorptionHearts;

    return Math.min(((Player) event.getEntity()).getMaxHealth(), realFinalDamage);
  }
}
