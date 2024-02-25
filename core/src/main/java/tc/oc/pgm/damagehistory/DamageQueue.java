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

  public double getAbsorptionHeartsRemaining() {
    return absorptionHeartsRemaining;
  }

  public void reduceAbsorptionHeartsTotal(double amount) {
    System.out.println("total reduced by " + amount);
    this.absorptionHeartsTotal = this.absorptionHeartsTotal - amount;
  }

  public void setAbsorptionHearts(double absorptionHearts) {
    this.absorptionHeartsTotal = absorptionHearts;
    this.absorptionHeartsRemaining = absorptionHearts;
  }

  public void resetAbsorptionHearts() {
    this.absorptionHeartsTotal = 0;
    this.absorptionHeartsRemaining = 0;
  }

  public boolean shouldTick() {
    return absorptionHeartsRemaining == 0 && absorptionHeartsTotal != 0;
  }

  public void reduceAbsorb(double damageAmount) {
    this.absorptionHeartsRemaining = Math.max(0, absorptionHeartsRemaining - damageAmount);
  }

  public DamageQueue clamp(double clampValue) {
    DamageQueue result = new DamageQueue();

    double totalDamage = 0;
    // Iterate through the damage entries from the start of the deque
    for (DamageEntry entry : this) {
      // Check if adding this entry's damage exceeds the clamp value
      if (totalDamage + entry.getDamage() > clampValue) {
        // Calculate the remaining damage needed to meet the clamp value
        double remainingDamage = clampValue - totalDamage;
        // Remove damage from this entry
        entry.removeDamage(remainingDamage);
        // Add this entry to the result deque
        result.add(entry);
        // No need to iterate further, as the clamp value is reached
        break;
      } else {
        // Add this entry to the result deque
        result.add(entry);
        // Add this entry's damage to the total
        totalDamage += entry.getDamage();
      }
    }

    return result;
  }
}
