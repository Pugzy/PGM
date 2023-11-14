package tc.oc.pgm.damagehistory;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;

public class DamageHistory {

  private final Map<UUID, Deque<DamageEntry>> allPlayerDamage = new HashMap<>();

  public DamageHistory() {}

  public final Deque<DamageEntry> getPlayerHistory(UUID uuid) {
    return allPlayerDamage.computeIfAbsent(uuid, item -> new LinkedList<>());
  }

  public void addDamage(
      MatchPlayer target, double damageAmount, @Nullable ParticipantState attacker) {
    Deque<DamageEntry> playerHistory = getPlayerHistory(target.getId());

    // Update existing if same player causing damage
    if (!playerHistory.isEmpty()) {
      DamageEntry last = playerHistory.getLast();
      if (shouldMergeParticipants(last.getPlayer(), attacker)) {
        last.addDamage(attacker, damageAmount);
        return;
      }
    }

    playerHistory.addLast(new DamageEntry(attacker, damageAmount));
  }

  public void removeDamage(MatchPlayer target, double damageAmount) {
    Deque<DamageEntry> playerHistory = getPlayerHistory(target.getId());
    if (playerHistory.isEmpty()) return;

    double subtractAmount = damageAmount;
    while (!playerHistory.isEmpty() && subtractAmount > 0) {
      DamageEntry first = playerHistory.getFirst();
      if (first.getDamage() < subtractAmount + 0.00001) { // make 0.0...1 const epsilon
        subtractAmount -= first.getDamage();
        playerHistory.removeFirst();
      } else {
        first.removeDamage(subtractAmount);
        break;
      }
    }
  }

  public boolean shouldMergeParticipants(ParticipantState firstItem, ParticipantState secondItem) {
    if (firstItem == null || secondItem == null) return firstItem == secondItem;

    // Only allow if they share the same UUID and party
    if (!firstItem.getId().equals(secondItem.getId())) return false;
    return (firstItem.getParty().equals(secondItem.getParty()));
  }
}
