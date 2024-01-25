package tc.oc.pgm.damagehistory;

import java.util.ArrayDeque;

public class DamageQueue extends ArrayDeque<DamageEntry> {

  private double absorptionHeartsTotal;
  private double absorptionHeartsRemaining;

  public DamageQueue() {
    this.absorptionHeartsTotal = 0;
    this.absorptionHeartsRemaining = 0;
  }

  public double getAbsorptionHeartsTotal() {
    return absorptionHeartsTotal;
  }

  public void setAbsorptionHearts(double absorptionHearts) {
    this.absorptionHeartsTotal = absorptionHearts;
    this.absorptionHeartsRemaining = absorptionHearts;
  }

  public void resetAbsorptionHearts() {
    this.absorptionHeartsTotal = 0;
    this.absorptionHeartsRemaining = 0;
  }

  public void reduceAbsorb(double damageAmount) {
    this.absorptionHeartsRemaining = Math.max(0, absorptionHeartsRemaining - damageAmount);
  }
}
