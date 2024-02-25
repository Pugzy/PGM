package tc.oc.pgm.damagehistory;

import static net.kyori.adventure.text.Component.text;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;

public class DamageHistory {

  public static final double EPSILON = 0.00001;

  private final Map<UUID, DamageQueue> allPlayerDamage = new HashMap<>();

  public DamageHistory() {}

  public DamageQueue getPlayerHistory(UUID uuid) {
    return allPlayerDamage.computeIfAbsent(uuid, item -> new DamageQueue());
  }

  public Map<UUID, DamageQueue> getAllPlayerDamage() {
    return allPlayerDamage;
  }

  public void addDamage(
      MatchPlayer target, double damageAmount, @Nullable ParticipantState attacker) {
    if (damageAmount == 0) return;
    target.sendMessage(text("Damaged " + damageAmount));
    DamageQueue playerHistory = getPlayerHistory(target.getId());

    // When they take damage when they have absorbtion
    playerHistory.reduceAbsorb(damageAmount);

    // Update existing if same player causing damage
    if (!playerHistory.isEmpty()) {
      DamageEntry last = playerHistory.getLast();
      if (shouldMergeParticipants(last.getDamager(), attacker)) {
        last.addDamage(attacker, damageAmount);
        return;
      }
    }

    playerHistory.addLast(new DamageEntry(attacker, damageAmount));
  }

  public void removeDamage(MatchPlayer target, double damageAmount) {
    if (damageAmount == 0) return;
    target.sendMessage(text("Damage removed " + damageAmount));
    DamageQueue playerHistory = getPlayerHistory(target.getId());
    if (playerHistory.isEmpty()) return;

    double subtractAmount = damageAmount;
    while (!playerHistory.isEmpty() && subtractAmount > 0) {
      DamageEntry first = playerHistory.getFirst();
      if (first.getDamage() < subtractAmount + EPSILON) {
        subtractAmount -= first.getDamage();
        playerHistory.removeFirst();
      } else {
        first.removeDamage(subtractAmount);
        break;
      }
    }
  }

  public void reduceAbsorb(UUID uuid, double amount) {
    DamageQueue playerHistory = getPlayerHistory(uuid);
    playerHistory.reduceAbsorptionHeartsTotal(amount);
  }

  public void clampDamageValues(MatchPlayer player, double health) {}

  public void setAbsortb(MatchPlayer target, double amount) {
    target.sendMessage(text("Set absorbtion " + amount));
    DamageQueue playerHistory = getPlayerHistory(target.getId());
    playerHistory.setAbsorptionHearts(amount);
  }

  public void removeAbsortb(MatchPlayer target) {
    target.sendMessage(text("Reset absorbtion"));
    DamageQueue playerHistory = getPlayerHistory(target.getId());
    playerHistory.resetAbsorptionHearts();
  }

  public boolean shouldMergeParticipants(ParticipantState firstItem, ParticipantState secondItem) {
    if (firstItem == null || secondItem == null) return firstItem == secondItem;

    // Only allow if they share the same UUID and party
    if (!firstItem.getId().equals(secondItem.getId())) return false;
    return (firstItem.getParty().equals(secondItem.getParty()));
  }
}
