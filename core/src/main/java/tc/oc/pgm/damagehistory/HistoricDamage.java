package tc.oc.pgm.damagehistory;

import javax.annotation.Nullable;
import tc.oc.pgm.api.player.ParticipantState;

public class HistoricDamage {

  @Nullable private final ParticipantState player;

  private double damage;

  public HistoricDamage(@Nullable ParticipantState player, double damage) {
    this.player = player;
    this.damage = damage;
  }

  @Nullable
  public ParticipantState getPlayer() {
    return player;
  }

  public double getDamage() {
    return damage;
  }

  public void addDamage(double damage) {
    this.damage += damage;
  }
}
