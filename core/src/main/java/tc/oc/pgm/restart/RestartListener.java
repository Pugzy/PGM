package tc.oc.pgm.restart;

import java.util.Iterator;
import java.util.logging.Logger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.countdowns.SingleCountdownContext;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.util.ClassLogger;

public class RestartListener implements Listener {

  private final PGM plugin;
  private final MatchManager matchManager;
  private final Logger logger;

  private long matchCount;

  private @Nullable RequestRestartEvent.Deferral playingDeferral;

  static RestartListener instance;

  public RestartListener(PGM plugin, MatchManager matchManager) {
    this.plugin = plugin;
    this.matchManager = matchManager;
    this.logger = ClassLogger.get(plugin.getLogger(), getClass());
    instance = this;
  }

  public static RestartListener getInstance() {
    return instance;
  }

  @EventHandler(ignoreCancelled = true)
  public void onRequestRestart(RequestRestartEvent event) {
    // Don't do a countdown if there's nobody online
    if (this.plugin.getServer().getOnlinePlayers().isEmpty()) return;

    Iterator<Match> iterator = matchManager.getMatches();
    Match match = iterator.hasNext() ? iterator.next() : null;
    if (match == null) return;

    if (match.isRunning()) {
      this.playingDeferral = event.defer(this.plugin);
      attemptMatchEnd(match);
    }
  }

  private void attemptMatchEnd(Match match) {
    if (playingDeferral == null) return;

    if (match.isRunning()) {
      if (match.getParticipants().isEmpty()) {
        this.logger.info("Ending empty match due to restart request");
        match.finish();
      }
    }
  }

  @EventHandler
  public void onCancelRestart(CancelRestartEvent event) {
    Iterator<Match> iterator = matchManager.getMatches();
    Match match = iterator.hasNext() ? iterator.next() : null;
    if (match != null) {
      SingleCountdownContext ctx = (SingleCountdownContext) match.getCountdown();
      if (ctx.getCountdown(RestartCountdown.class) != null) {
        this.logger.info("Cancelling restart countdown");
        ctx.cancelAll();
      }
    }
    RestartManager.getInstance().cancelRestart();
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onMatchEnd(MatchFinishEvent event) {
    if (this.playingDeferral != null) {
      this.playingDeferral.remove();
      this.playingDeferral = null;
    }
  }

  @EventHandler
  public void onPartyChange(PlayerPartyChangeEvent event) {
    attemptMatchEnd(event.getMatch());
  }

  @EventHandler
  public void onMatchLoad(MatchLoadEvent event) {
    long matchLimit = plugin.getConfiguration().getMatchLimit();
    if (++matchCount >= matchLimit && matchLimit > 0) {
      RestartManager.getInstance().queueRestart("Reached match limit of " + matchLimit);
    }
  }
}
