package tc.oc.pgm.restart;

import java.time.Duration;
import java.util.Iterator;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
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
import tc.oc.pgm.events.CountdownEndEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.util.ClassLogger;

public class RestartListener implements Listener {

  private final PGM plugin;
  private final MatchManager matchManager;
  private final Logger logger;

  private long matchCount;
  private boolean countdownComplete = false;

  private @Nullable RequestRestartEvent.Deferral playingDeferral;
  private @Nullable RequestRestartEvent.Deferral countdownDeferral;

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

  private void attemptMatchEnd(Match match) {
    if (match.isRunning()) {
      if (match.getParticipants().isEmpty()) {
        this.logger.info("Ending empty match due to restart request");
        match.finish();
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onRestartCountdownEnd(CountdownEndEvent event) {
    if (!(event.getCountdown() instanceof RestartCountdown)) return;

    releaseCountdownDeferral();
  }

  @EventHandler
  public void onRequestRestart(RequestRestartEvent event) {
    if (this.plugin.getServer().getOnlinePlayers().isEmpty()) return;

    Iterator<Match> iterator = matchManager.getMatches();
    Match match = iterator.hasNext() ? iterator.next() : null;
    if (match != null) {

      // Always create a deferral so we can do a countdown
      this.countdownDeferral = event.defer(this.plugin);

      if (match.isRunning()) {
        this.playingDeferral = event.defer(this.plugin);
        attemptMatchEnd(match);
      } else {
        startRestartCountdown(match);
      }
    }
  }

  public void startRestartCountdown(Match match) {
    if (!match.isRunning() && this.countdownDeferral != null) {

      if (RestartManager.isDeferredByOther(countdownDeferral)) return;

      if (!match.isRunning() && !countdownComplete) {
        // Start restart countdown if running
        SingleCountdownContext ctx = (SingleCountdownContext) match.getCountdown();
        ctx.cancelAll();

        Duration countdownTime =
            RestartManager.getCountdown() != null
                ? RestartManager.getCountdown()
                : PGM.get().getConfiguration().getRestartTime();
        this.logger.info("Starting restart countdown from " + countdownTime);
        ctx.start(new RestartCountdown(match), countdownTime);
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
    RestartManager.cancelRestart();
    this.countdownComplete = false;
    releaseCountdownDeferral();
  }

  private void releaseCountdownDeferral() {
    if (this.countdownDeferral != null) {
      this.countdownDeferral.remove();
      this.countdownDeferral = null;
    }
  }

  private void releasePlayingDeferral() {
    if (this.playingDeferral != null) {
      this.playingDeferral.remove();
      this.playingDeferral = null;
    }
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onMatchEnd(MatchFinishEvent event) {
    releasePlayingDeferral();

    // When restart queued but not caught by the listener
    if (RestartManager.isQueued() && countdownDeferral == null) requestRestart();

    // When restart caught start countdown
    if (countdownDeferral != null) startRestartCountdown(event.getMatch());
  }

  public void restartIfRequired() {
    // If no restart requested, don't restart
    if (!RestartManager.isQueued()) return;

    // If there are deferrals don't restart
    if (RestartManager.isDeferred()) return;

    Bukkit.getServer().broadcastMessage("shutdown");
    // Bukkit.getServer().shutdown();
  }

  public void requestRestart() {
    // Don't start a second restart
    if (countdownDeferral != null) return;

    this.plugin
        .getServer()
        .getPluginManager()
        .callEvent(new RequestRestartEvent(this::restartIfRequired));

    this.restartIfRequired();
  }

  @EventHandler
  public void onPartyChange(PlayerPartyChangeEvent event) {
    attemptMatchEnd(event.getMatch());
  }

  @EventHandler
  public void onMatchLoad(MatchLoadEvent event) {
    long matchLimit = plugin.getConfiguration().getMatchLimit();
    if (++matchCount >= matchLimit && matchLimit > 0) {
      RestartManager.queueRestart("Reached match limit of " + matchLimit);
    }
  }
}
