package tc.oc.pgm.damagehistory;

import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.api.player.MatchPlayer;

public class HistoricTracker {

  private final Map<UUID, Deque<HistoricDamage>> allPlayerDamage = new HashMap<>();

  public HistoricTracker() {}

  public void addDamage(MatchPlayer target, double damageAmount, @Nullable MatchPlayer attacker) {

    Deque<HistoricDamage> playerHistory = getPlayerHistory(target.getId());

    // Update existing if same player causing damage
    if (!playerHistory.isEmpty()) {
      HistoricDamage last = playerHistory.getLast();
      if (Objects.equals(last.getPlayer(), attacker)) {
        last.addDamage(damageAmount);
        target.getMatch().sendMessage(this.broadcast(target, playerHistory));
        return;
      }
    }

    playerHistory.addLast(new HistoricDamage(attacker, damageAmount));

    target.getMatch().sendMessage(this.broadcast(target, playerHistory));
  }

  public void removeDamage(MatchPlayer target, double damageAmount) {
    Deque<HistoricDamage> playerHistory = getPlayerHistory(target.getId());
    if (playerHistory.isEmpty()) return;

    double subtractAmount = damageAmount;
    while (!playerHistory.isEmpty() && subtractAmount > 0) {

      HistoricDamage first = playerHistory.getFirst();
      if (first.getDamage() < subtractAmount + 0.00001) { // make 0.0...1 const epsilon
        subtractAmount -= first.getDamage();
        playerHistory.removeFirst();
      } else {
        first.addDamage(-subtractAmount);
        break;
      }
    }

    target.getMatch().sendMessage(this.broadcast(target, playerHistory));
  }

  public final Deque<HistoricDamage> getPlayerHistory(UUID uuid) {
    return allPlayerDamage.computeIfAbsent(uuid, item -> new LinkedList<>());
  }

  public @NotNull TextComponent broadcast(MatchPlayer player, Deque<HistoricDamage> playerHistory) {

    TextComponent.Builder component = text();

    component.append(
        player
            .getName()
            .append(
                text(" Damage History:", NamedTextColor.YELLOW, TextDecoration.BOLD)
                    .append(newline())));

    playerHistory.forEach(
        item -> {
          System.out.println(item);
          component
              .append(text(" - "))
              .append(item.getPlayer() != null ? item.getPlayer().getName() : text("Unknown"))
              .append(text(" \u2764 " + item.getDamage(), NamedTextColor.RED))
              .append(newline());
        });

    return component.build();
  }
}
