package tc.oc.pgm.damagehistory;

import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.tracker.info.DamageInfo;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.spawns.events.ParticipantDespawnEvent;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.util.Pair;
import tc.oc.pgm.util.nms.NMSHacks;

@ListenerScope(MatchScope.RUNNING)
public class DamageHistoryMatchModule implements MatchModule, Listener {

  private final Match match;
  private final Logger logger;
  private final HistoricTracker historicTracker;

  public DamageHistoryMatchModule(Match match) {
    this.match = match;
    this.logger = match.getLogger();
    this.historicTracker = new HistoricTracker();
  }

  TrackerMatchModule tracker() {
    return match.needModule(TrackerMatchModule.class);
  }

  public @Nullable ParticipantState getAssister(MatchPlayer player) {
    Deque<HistoricDamage> damageHistory = this.historicTracker.getPlayerHistory(player.getId());
    if (damageHistory == null || damageHistory.size() <= 1) return null;

    ParticipantState killer = damageHistory.getLast().getPlayer();
    if (killer == null) return null;

    double health = damageHistory.stream().mapToDouble(HistoricDamage::getDamage).sum();

    Collections.reverse((List<?>) damageHistory);

    Set<Map.Entry<Pair<UUID, Competitor>, Double>> entries =
        damageHistory.stream()
            // Filter out damage without players, or damage from self or killer
            .filter(
                historicDamage -> {
                  ParticipantState damager = historicDamage.getPlayer();
                  return !(damager == null
                      || damager.getId().equals(player.getId())
                      || damager.getId().equals(killer.getId()));
                })
            .collect(
                Collectors.groupingBy(
                    historicDamage ->
                        Pair.of(
                            historicDamage.getPlayer().getId(),
                            historicDamage.getPlayer().getParty()),
                    Collectors.mapping(
                        HistoricDamage::getDamage, Collectors.reducing(0d, Double::sum))))
            .entrySet();

    entries.forEach(
        entry -> logger.log(Level.INFO, entry.getKey().getLeft() + ": " + entry.getValue()));

    Map.Entry<Pair<UUID, Competitor>, Double> highestDamager =
        entries.stream().max(Map.Entry.comparingByValue()).orElse(null);

    if (highestDamager == null
        || highestDamager.getValue() < (health * PGM.get().getConfiguration().getAssistPercent()))
      return null;

    // Make sure player exists on same team as damage dealt as an enemy
    MatchPlayer assister = player.getMatch().getPlayer(highestDamager.getKey().getLeft());
    if (assister == null
        || !assister.getParty().equals(highestDamager.getKey().getRight())
        || assister.getParty().equals(player.getParty())) return null;

    return assister.getParticipantState();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onDamage(EntityDamageEvent event) {
    MatchPlayer victim = getVictim(event.getEntity());
    if (victim == null) return;

    DamageInfo damageInfo = tracker().resolveDamage(event);

    // Store damage with value
    ParticipantState attacker = damageInfo.getAttacker() != null ? damageInfo.getAttacker() : null;

    historicTracker.addDamage(victim, getDamageAmount(event), attacker);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDespawn(final ParticipantDespawnEvent event) {
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
