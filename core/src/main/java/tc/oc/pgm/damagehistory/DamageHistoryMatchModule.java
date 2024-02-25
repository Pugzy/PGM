package tc.oc.pgm.damagehistory;

import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;

import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PotionEffectAddEvent;
import org.bukkit.event.entity.PotionEffectExpireEvent;
import org.bukkit.event.entity.PotionEffectRemoveEvent;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.Tickable;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.api.tracker.info.DamageInfo;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.kits.ApplyKitEvent;
import tc.oc.pgm.kits.HealthKit;
import tc.oc.pgm.kits.MaxHealthKit;
import tc.oc.pgm.spawns.events.ParticipantDespawnEvent;
import tc.oc.pgm.tracker.TrackerMatchModule;

@ListenerScope(MatchScope.RUNNING)
public class DamageHistoryMatchModule implements MatchModule, Listener, Tickable {

  private final Match match;
  private final DamageHistory damageHistory;

  public DamageHistoryMatchModule(Match match) {
    this.match = match;
    this.damageHistory = new DamageHistory();
  }

  TrackerMatchModule tracker() {
    return match.needModule(TrackerMatchModule.class);
  }

  public DamageQueue getDamageHistory(MatchPlayer player) {
    return this.damageHistory.getPlayerHistory(player.getId());
  }

  @Override
  public void tick(Match match, Tick tick) {
    if (!match.isRunning()) return;

    if (tick.tick % 20 != 0) return;

    damageHistory
        .getAllPlayerDamage()
        .forEach(
            (uuid, damageEntries) -> {
              if (damageEntries.shouldTick()) {
                MatchPlayer player = match.getPlayer(uuid);
                if (player == null) return;
                if (player.getBukkit().getHealth() == player.getBukkit().getMaxHealth()) {
                  damageHistory.reduceAbsorb(uuid, 0.25);
                }
              }
            });
  }

  public @Nullable ParticipantState getAssister(MatchPlayer player) {
    DamageQueue damageHistory = getDamageHistory(player);
    if (damageHistory == null || damageHistory.size() <= 1) return null;

    ParticipantState killer = damageHistory.getLast().getDamager();
    if (killer == null) return null;

    double damageReceived = player.getBukkit().getMaxHealth() + damageHistory.getAbsorptionHeartsTotal();

    System.out.println("total for player: " + damageReceived);

    Collections.reverse((List<?>) damageHistory);

    damageHistory = damageHistory.clamp(damageReceived);

    Set<Map.Entry<DamageHistoryKey, Double>> entries =
        damageHistory.stream()
            // Filter out damage without players, or damage from self or killer
            .filter(
                historicDamage -> {
                  ParticipantState damager = historicDamage.getDamager();
                  return !(damager == null
                      || damager.getId().equals(player.getId())
                      || damager.getId().equals(killer.getId()));
                })
            .collect(
                Collectors.groupingBy(
                    DamageHistoryKey::from,
                    Collectors.mapping(
                        DamageEntry::getDamage, Collectors.reducing(0d, Double::sum))))
            .entrySet();

    Map.Entry<DamageHistoryKey, Double> highestDamager =
        entries.stream().max(Map.Entry.comparingByValue()).orElse(null);

    if (highestDamager == null
        || highestDamager.getValue()
            < (damageReceived * PGM.get().getConfiguration().getAssistPercent())
        || highestDamager.getKey().getParty().equals(player.getParty())) return null;

    return highestDamager.getKey().getState();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onDamage(EntityDamageEvent event) {
    MatchPlayer victim = getTarget(event.getEntity());
    if (victim == null) return;

    DamageInfo damageInfo = tracker().resolveDamage(event);
    ParticipantState attacker = damageInfo.getAttacker() != null ? damageInfo.getAttacker() : null;

    damageHistory.addDamage(victim, getDamageAmount(event), attacker);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDespawn(final ParticipantDespawnEvent event) {
    DamageQueue damageHistory = getDamageHistory(event.getPlayer());
    event.getPlayer().sendMessage(text(event.getPlayer().getBukkit().getMaxHealth() + " - " + damageHistory.getAbsorptionHeartsTotal()));
    broadcast(event.getPlayer(), damageHistory);
    getDamageHistory(event.getPlayer()).clear();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerHeal(final EntityRegainHealthEvent event) {
    MatchPlayer target = getTarget(event.getEntity());
    if (target == null) return;

    double maxHealing = target.getBukkit().getMaxHealth() - target.getBukkit().getHealth();

    damageHistory.removeDamage(target, Math.min(maxHealing, event.getAmount()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPotionEffectAdd(final PotionEffectAddEvent event) {
    if (!event.getEffect().getType().equals(PotionEffectType.ABSORPTION)) return;

    MatchPlayer target = getTarget(event.getEntity());
    if (target == null) return;

    double newHearts = (event.getEffect().getAmplifier() + 1) * 4;

    damageHistory.setAbsortb(target, newHearts);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPotionEffectExpire(final PotionEffectExpireEvent event) {
    if (!event.getEffect().getType().equals(PotionEffectType.ABSORPTION)) return;

    MatchPlayer target = getTarget(event.getEntity());
    if (target == null) return;

    damageHistory.removeAbsortb(target);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPotionEffectRemove(final PotionEffectRemoveEvent event) {
    if (!event.getEffect().getType().equals(PotionEffectType.ABSORPTION)) return;

    MatchPlayer target = getTarget(event.getEntity());
    if (target == null) return;

    damageHistory.removeAbsortb(target);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPotionEffectAddHealth(final PotionEffectAddEvent event) {
    if (!event.getEffect().getType().equals(PotionEffectType.HEALTH_BOOST)) return;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onHealthChangeKit(ApplyKitEvent event) {
    if (!(event.getKit() instanceof HealthKit)) return;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMaxHealthChangeKit(ApplyKitEvent event) {
    if (!(event.getKit() instanceof MaxHealthKit)) return;

    double health = event.getPlayer().getBukkit().getHealth();
    double maxHealth = event.getPlayer().getBukkit().getMaxHealth();

    if (health >= maxHealth) {
      damageHistory.clampDamageValues(event.getPlayer(), health);
    }
  }

  public void broadcast(MatchPlayer player, Deque<DamageEntry> damageHistory) {

    TextComponent.Builder component = text();

    component
        .append(player.getName())
        .append(
            text(" Damage History:", NamedTextColor.YELLOW, TextDecoration.BOLD).append(newline()));

    damageHistory.forEach(
        item -> {
          component
              .append(text(" - "))
              .append(item.getDamager() != null ? item.getDamager().getName() : text("Unknown"))
              .append(text(" \u2764 " + item.getDamage(), NamedTextColor.RED))
              .append(newline());
        });

    player.getMatch().sendMessage(component.build());
  }

  @Nullable
  MatchPlayer getTarget(Entity entity) {
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
