package tc.oc.pgm.damagehistory;

import static tc.oc.pgm.util.Assert.assertNotNull;

import java.util.Objects;
import java.util.UUID;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.ParticipantState;

public class DamageHistoryKey {

  private final UUID player;
  private final Competitor party;
  private final ParticipantState state;

  public DamageHistoryKey(UUID player, Competitor party, ParticipantState state) {
    this.player = player;
    this.party = party;
    this.state = state;
  }

  public static DamageHistoryKey from(DamageEntry damageEntry) {
    ParticipantState damager = damageEntry.getPlayer();
    assertNotNull(damager);
    return new DamageHistoryKey(damager.getId(), damager.getParty(), damager);
  }

  public UUID getPlayer() {
    return player;
  }

  public Competitor getParty() {
    return party;
  }

  public ParticipantState getState() {
    return state;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DamageHistoryKey that = (DamageHistoryKey) o;
    return Objects.equals(player, that.player) && Objects.equals(party, that.party);
  }

  @Override
  public int hashCode() {
    return Objects.hash(player, party);
  }
}
