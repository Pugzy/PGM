package tc.oc.pgm.damagehistory;

import javax.annotation.Nullable;
import tc.oc.pgm.api.player.MatchPlayer;

public class HistoricDamage {

  @Nullable private final MatchPlayer player;

  private double damage;

  public HistoricDamage(@Nullable MatchPlayer player, double damage) {
    this.player = player;
    this.damage = damage;
  }

  @Nullable
  public MatchPlayer getPlayer() {
    return player;
  }

  public double getDamage() {
    return damage;
  }

  public void addDamage(double damage) {
    this.damage += damage;
  }
}
