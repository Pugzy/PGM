package tc.oc.pgm.spawner;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;

@FeatureInfo(name = "spawner")
public class SpawnerDefinition extends SelfIdentifyingFeatureDefinition {
  public final Region spawnRegion;
  public final Region playerRegion;
  public final int maxEntities;
  public final Duration initialDelay, minDelay, maxDelay, delay;
  public final List<Spawnable> objects;
  public final Filter playerFilter;
  // TODO: failReset not used but should allow items that fail the max entities check to reset their countdown
  public final boolean failReset;
  public final TickCondition tickCondition;

  public SpawnerDefinition(
      String id,
      List<Spawnable> objects,
      Region spawnRegion,
      Region playerRegion,
      Filter playerFilter,
      Duration initialDelay,
      Duration delay,
      Duration minDelay,
      Duration maxDelay,
      boolean failReset,
      int maxEntities,
      TickCondition tickCondition) {
    super(id);
    this.spawnRegion = spawnRegion;
    this.playerRegion = playerRegion;
    this.maxEntities = maxEntities;
    this.initialDelay = initialDelay;
    this.delay = delay;
    this.minDelay = minDelay;
    this.maxDelay = maxDelay;
    this.failReset = failReset;
    this.objects = objects;
    this.playerFilter = playerFilter;
    this.tickCondition = tickCondition;
  }

  public enum TickCondition {
    INSIDE, // - Ticks down only when players are in the region
    ALWAYS, // - Ticks down all the time (only spawns when in region)
    RESETS, // - Ticks reset when no player in region
  }

  @Override
  protected String getDefaultId() {
    return super.makeDefaultId();
  }

  public static String makeDefaultId(@Nullable String name, AtomicInteger serial) {
    return "--"
        + makeTypeName(SpawnerDefinition.class)
        + "-"
        + (name != null ? makeId(name) : serial.getAndIncrement());
  }
}
