package tc.oc.pgm.portals;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.feature.FeatureDefinition;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.FilterMatchModule;
import tc.oc.pgm.util.chat.Sound;

public class Portal implements FeatureDefinition {

  protected final PortalTransform transform;
  protected final Filter trigger;
  protected final Filter participantFilter;
  protected final Filter observerFilter;
  protected final boolean sound;
  protected final boolean smooth;

  public Portal(
          Filter trigger,
          PortalTransform transform,
          Filter participantFilter,
          Filter observerFilter,
          boolean sound,
          boolean smooth) {

    this.transform = transform;
    this.trigger = trigger;
    this.participantFilter = participantFilter;
    this.observerFilter = observerFilter;
    this.sound = sound;
    this.smooth = smooth;
  }

  public void load(FilterMatchModule fmm) {
    fmm.onRise(
        PlayerQuery.class,
        trigger,
        playerQuery -> {
          MatchPlayer matchPlayer = playerQuery.getPlayer();
          if (matchPlayer != null && canUse(matchPlayer) && PortalMatchModule.teleported(matchPlayer)) {
            teleportPlayer(matchPlayer, matchPlayer.getBukkit().getLocation());
          }
          ;
        });
  }

  protected boolean canUse(MatchPlayer player) {
    return (player.isParticipating() ? participantFilter : observerFilter)
        .query(player.getQuery())
        .isAllowed();
  }

  protected void teleportPlayer(final MatchPlayer player, final Location from) {
    System.out.println(this);
    System.out.println(from);
    System.out.println(transform);

    final Location to = transform.apply(from);

    System.out.println(to);

    System.out.println("--------------------------------");

    final Player bukkit = player.getBukkit();
    final Match match = player.getMatch();

    final Vector delta;
    final float deltaYaw, deltaPitch;
    if (this.smooth) {
      Location location = bukkit.getLocation();
      delta = to.toVector().subtract(location.toVector());
      deltaYaw = to.getYaw() - location.getYaw();
      deltaPitch = to.getPitch() - location.getPitch();
    } else {
      delta = null;
      deltaYaw = deltaPitch = Float.NaN;
    }

    if (this.sound) {
      // Don't play the sound for the teleporting player at the entering portal,
      // because they will instantly teleport away and hear the one at the exit.
      for (MatchPlayer listener : match.getPlayers()) {
        if (listener != player && listener.getBukkit().canSee(player.getBukkit())) {
          listener.playSound(
              new Sound("mob.endermen.portal", 1f, 1f, bukkit.getLocation().toVector()));
        }
      }
    }

    // Defer because some things break if a player teleports during a move event
    match
        .getExecutor(MatchScope.LOADED)
        .execute(
            () -> {
              if (!bukkit.isOnline()) return;

              // Use ENDER_PEARL as the cause so that this teleport is treated
              // as an "in-game" movement
              if (delta == null) {
                bukkit.teleport(to, PlayerTeleportEvent.TeleportCause.ENDER_PEARL);
              } else {
                bukkit.teleportRelative(
                    delta, deltaYaw, deltaPitch, PlayerTeleportEvent.TeleportCause.ENDER_PEARL);
              }

              // Reset fall distance
              bukkit.setFallDistance(0);

              if (Portal.this.sound) {
                for (MatchPlayer listener : match.getPlayers()) {
                  if (listener.getBukkit().canSee(player.getBukkit())) {
                    listener.playSound(
                        new Sound("mob.endermen.portal", 1f, 1f, to.toVector()));
                  }
                }
              }
            });
  }
}
