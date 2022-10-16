package tc.oc.pgm.spawner;

import java.util.Objects;
import org.bukkit.Bukkit;
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

  private long ticksUntilSpawn;
  private Long lastSpawnTick;
  private long currentDelay;
  private long spawnedEntities;

  public Spawner(SpawnerDefinition definition, Match match) {
    this.definition = definition;
    this.match = match;
    this.lastSpawnTick = null;
    this.ticksUntilSpawn = TimeUtils.toTicks(definition.initialDelay);
    this.playerTracker = new RegionPlayerTracker(match, definition.playerRegion);
    calculateDelay();
  }

  @Override
  public void tick(Match match, Tick tick) {
    if (!canSpawn()) return;

    ticksUntilSpawn--;



    if (lastSpawnTick == null) lastSpawnTick = match.getTick().tick;

    //Bukkit.broadcastMessage("" + ticksUntilSpawn);
    boolean earlySpawn = match.getTick().tick - lastSpawnTick > ticksUntilSpawn;
    boolean countdownSpawn = ticksUntilSpawn > 0;

    if ((definition.reset && !countdownSpawn) || !earlySpawn) return;

    for (Spawnable spawnable : definition.objects) {
      final Location location =
              definition.spawnRegion.getRandom(match.getRandom()).toLocation(match.getWorld());
      spawnable.spawn(location, match);
      match.getWorld().spigot().playEffect(location, Effect.FLAME, 0, 0, 0, 0.15f, 0, 0, 40, 64);
      spawnedEntities = spawnedEntities + spawnable.getSpawnCount();
    }

    calculateDelay();
  }

  private void calculateDelay() {
    if (lastSpawnTick == null) {
      ticksUntilSpawn = TimeUtils.toTicks(definition.initialDelay);
    } else if (definition.minDelay == definition.maxDelay) {
      ticksUntilSpawn = TimeUtils.toTicks(definition.delay);
    } else {
      long maxDelay = TimeUtils.toTicks(definition.maxDelay);
      long minDelay = TimeUtils.toTicks(definition.minDelay);
      ticksUntilSpawn =
          (long)
              (match.getRandom().nextDouble() * (maxDelay - minDelay)
                  + minDelay); // Picks a random tick duration between minDelay and maxDelay
    }

    Bukkit.broadcastMessage("Next " + definition.getId() + " spawn in " + ticksUntilSpawn);
  }

  private boolean canSpawn() {
    if (spawnedEntities >= definition.maxEntities || this.playerTracker.getPlayers().isEmpty()) {
      if (this.definition.reset) {
        lastSpawnTick = match.getTick().tick;
      }
      return false;
    }
    for (MatchPlayer player : this.playerTracker.getPlayers()) {
      if (definition.playerFilter.query(player).isAllowed()) return true;
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

  public void registerEvents() {
    this.match.addListener(this.playerTracker, MatchScope.RUNNING);
  }

  public void unregisterEvents() {
    HandlerList.unregisterAll(this.playerTracker);
  }
}
