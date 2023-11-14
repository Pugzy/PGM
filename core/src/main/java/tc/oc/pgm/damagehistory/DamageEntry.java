package tc.oc.pgm.damagehistory;

import javax.annotation.Nullable;
import tc.oc.pgm.api.player.ParticipantState;

public class DamageEntry {

  @Nullable private ParticipantState player;
  private double damage;

  public DamageEntry(@Nullable ParticipantState player, double damage) {
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

  public void addDamage(@Nullable ParticipantState player, double damage) {
    this.player = player;
    this.damage += damage;
  }

  public void removeDamage(double damage) {
    this.damage -= damage;
  }
}
