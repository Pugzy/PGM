package tc.oc.pgm.command;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.damagehistory.DamageHistoryMatchModule;
import tc.oc.pgm.util.Audience;

public final class DamageCommand {

  @CommandMethod("damageof [target]")
  @CommandDescription("See a players damage history")
  public void print(
      Audience audience,
      DamageHistoryMatchModule damageHistory,
      @Argument("target") MatchPlayer target) {
    audience.sendMessage(damageHistory.broadcast(target, damageHistory.getDamageHistory(target)));
  }
}
