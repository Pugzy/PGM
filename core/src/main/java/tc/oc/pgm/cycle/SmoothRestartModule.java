package tc.oc.pgm.cycle;

import static net.kyori.adventure.text.Component.text;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.events.CountdownCancelEvent;
import tc.oc.pgm.events.CountdownEndEvent;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.restart.CancelRestartEvent;
import tc.oc.pgm.restart.RequestRestartEvent;
import tc.oc.pgm.restart.RestartListener;
import tc.oc.pgm.restart.RestartManager;

@ListenerScope(MatchScope.LOADED)
public class SmoothRestartModule implements MatchModule, Listener {

  private final Match match;

  private RequestRestartEvent restartEvent;
  private RequestRestartEvent.Deferral defer;

  private FakeCycleCountdown countdown;

  public SmoothRestartModule(Match match) {
    this.match = match;
  }

  public void updateSelectedMap(MapInfo nextMap) {
    if (nextMap == null) return;
    Bukkit.broadcastMessage(nextMap.toString());
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onRequestRestart(RequestRestartEvent event) {
    this.restartEvent = event;

    // Randomly allow for regular restarts
    if (match.getRandom().nextBoolean()) {
      match.sendMessage(text("real restart"));
      return;
    }

    match.sendMessage(text("fake cycle"));

    this.defer = event.defer(PGM.get());

    if (!match.isRunning()) tryStartCycleCountdown();
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onMatchEnd(MatchFinishEvent event) {
    tryStartCycleCountdown();
  }

  public void tryStartCycleCountdown() {
    if (!RestartManager.isQueued()) return;
    if (defer == null) return;

    countdown = new FakeCycleCountdown(match, this);
    match.getCountdown().start(countdown, PGM.get().getConfiguration().getCycleTime());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onCountdownCancel(CountdownCancelEvent event) {
    if (restartEvent == null || defer == null) return;

    if (!(event.getCountdown() instanceof FakeCycleCountdown)) return;

    defer.remove();

    this.restartEvent = null;
    this.defer = null;
  }

  @EventHandler
  public void onRestartCancel(CancelRestartEvent event) {
    if (match.getCountdown().isRunning(countdown)) {
      match.getCountdown().cancel(countdown);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onCountdownEnd(CountdownEndEvent event) {

    if (restartEvent == null || defer == null) return;

    if (!(event.getCountdown() instanceof FakeCycleCountdown)) return;

    Bukkit.getServer().broadcastMessage("next map: " + countdown.nextMap);

    event.getMatch().sendMessage(text("get the map"));

    defer.remove();

    RestartListener.getInstance().startRestartCountdown(event.getMatch());
  }

  public void mapSelected(MapInfo nextMap) {
  }
}
