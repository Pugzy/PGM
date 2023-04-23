package tc.oc.pgm.spawner;

import java.util.Objects;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.metadata.Metadatable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.Tickable;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.controlpoint.RegionPlayerTracker;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.bukkit.MetadataUtils;

public class Spawner implements Listener, Tickable {

  public static final String METADATA_KEY = "spawner";

  private final Match match;
  private final SpawnerDefinition definition;
  private final RegionPlayerTracker playerTracker;

  private Long currentDelay;
  private Long ticksUntilSpawn;
  private long spawnedEntities;

  public Spawner(SpawnerDefinition definition, Match match) {
    this.definition = definition;
    this.match = match;
    this.currentDelay = getInitialDelay();
    this.ticksUntilSpawn = currentDelay;
    this.playerTracker = new RegionPlayerTracker(match, definition.playerRegion);
  }

  @Override
  public void tick(Match match, Tick tick) {
    boolean canSpawn = canSpawn();

    if (canTick(canSpawn)) ticksUntilSpawn--;

    if (!canSpawn && definition.tickCondition.equals(SpawnerDefinition.TickCondition.RESETS)) {
      ticksUntilSpawn = currentDelay;
    }

    if (canSpawn && ticksUntilSpawn <= 0) {
      spawn();
      calculateDelay();
    }
  }

  private Long getInitialDelay() {
    if (definition.initialDelay != null) {
      return TimeUtils.toTicks(definition.initialDelay);
    }

    calculateDelay();

    return ticksUntilSpawn;
  }

  private void spawn() {
    for (Spawnable spawnable : definition.objects) {
      final Location location =
          definition.spawnRegion.getRandom(match.getRandom()).toLocation(match.getWorld());
      spawnable.spawn(location, match);
      match.getWorld().spigot().playEffect(location, Effect.FLAME, 0, 0, 0, 0.15f, 0, 0, 40, 64);
      spawnedEntities = spawnedEntities + spawnable.getSpawnCount();
    }
  }

  private void calculateDelay() {
    if (definition.minDelay == definition.maxDelay) {
      ticksUntilSpawn = TimeUtils.toTicks(definition.delay);
    } else {
      long maxDelay = TimeUtils.toTicks(definition.maxDelay);
      long minDelay = TimeUtils.toTicks(definition.minDelay);
      ticksUntilSpawn =
          (long)
              (match.getRandom().nextDouble() * (maxDelay - minDelay)
                  + minDelay); // Picks a random tick duration between minDelay and maxDelay
    }

    currentDelay = ticksUntilSpawn;
  }

  private boolean canTick(boolean canSpawn) {
    switch (this.definition.tickCondition) {
      case ALWAYS:
      default:
        return true;
      case INSIDE:
      case RESETS:
        return canSpawn;
    }
  }

  private boolean canSpawn() {
    if (spawnedEntities >= definition.maxEntities) {
      return false;
    }

    if (!this.playerTracker.getPlayers().isEmpty()) {
      for (MatchPlayer player : this.playerTracker.getPlayers()) {
        if (definition.playerFilter.query(player).isAllowed()) return true;
      }
    }

    return false;
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onItemMerge(ItemMergeEvent event) {
    boolean entityTracked = event.getEntity().hasMetadata(METADATA_KEY);
    boolean targetTracked = event.getTarget().hasMetadata(METADATA_KEY);
    if (!entityTracked && !targetTracked) return; // None affected
    if (entityTracked && targetTracked) {
      String entitySpawnerId =
          MetadataUtils.getMetadata(event.getEntity(), METADATA_KEY, PGM.get()).asString();
      String targetSpawnerId =
          MetadataUtils.getMetadata(event.getTarget(), METADATA_KEY, PGM.get()).asString();
      if (entitySpawnerId.equals(targetSpawnerId)) return; // Same spawner, allow merge
    }
    event.setCancelled(true);
  }

  private void handleEntityRemoveEvent(Metadatable metadatable, int amount) {
    if (metadatable.hasMetadata(METADATA_KEY)) {
      if (Objects.equals(
          MetadataUtils.getMetadata(metadatable, METADATA_KEY, PGM.get()).asString(),
          definition.getId())) {
        spawnedEntities -= amount;
        spawnedEntities = Math.max(0, spawnedEntities);
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onEntityDeath(EntityDeathEvent event) {
    handleEntityRemoveEvent(event.getEntity(), 1);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onItemDespawn(ItemDespawnEvent event) {
    handleEntityRemoveEvent(event.getEntity(), event.getEntity().getItemStack().getAmount());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPotionSplash(PotionSplashEvent event) {
    handleEntityRemoveEvent(event.getEntity(), 1);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerPickup(PlayerPickupItemEvent event) {
    handleEntityRemoveEvent(event.getItem(), event.getItem().getItemStack().getAmount());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchEnd(MatchFinishEvent event) {
    // TODO: turn off tracking on match end
    //    this.playerTracker.clear();
    //    this.players.disable();
  }

  public void registerEvents() {
    this.match.addListener(this.playerTracker, MatchScope.RUNNING);
  }

  public void unregisterEvents() {
    HandlerList.unregisterAll(this.playerTracker);
  }
}
