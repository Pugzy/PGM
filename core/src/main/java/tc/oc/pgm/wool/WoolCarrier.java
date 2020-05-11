package tc.oc.pgm.wool;

import com.google.common.collect.Sets;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.TimeUtils;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class WoolCarrier implements Runnable {

    final private MonumentWool wool;
    private final Set<Player> woolCarriers;

    private Future<?> task;

    public WoolCarrier(MonumentWool wool) {
        this.wool = wool;
        this.woolCarriers = Sets.newHashSet();
        this.task = null;
    }

    // Match Start Event
    public void start() {
        this.task =
                this.wool
                        .getMatch()
                        .getExecutor(MatchScope.LOADED)
                        .scheduleWithFixedDelay(this, TimeUtils.TICK, TimeUtils.toTicks(Duration.ofSeconds(2)), TimeUnit.MILLISECONDS);
    }

    // Match End Event
    public void end() {
        this.task = null;
    }

    @Override
    public void run() {
        this.tickLoaded();
        if ((this.wool.getMatch().isRunning()) && !this.wool.isCompleted())this.tickRunning();
    }

    private void tickLoaded() {
    }

    private void tickRunning() {
        broadcastParticles();
    }

    protected void broadcastParticles() {
        // Loop all players carrying the wool..
        for (Player carrier : this.woolCarriers) {
            for (MatchPlayer player : wool.getMatch().getPlayers()) {
                if (player.getBukkit() != carrier) {
//                System.out.println("Broadcasting " + carrier.getName() + " to " + player.getBukkit().getName());
                    player.getBukkit().spigot().playEffect(
                            carrier.getLocation().clone().add(0, 1, 0),
                            Effect.TILE_DUST,
                            Material.WOOL.getId(),
                            wool.getDyeColor().getWoolData(),
                            0.25f, // radius on each axis of the particle ball
                            0.5f,
                            0.25f,
                            0f, // initial horizontal velocity
                            1, // number of particles
                            64); // radius in blocks to show particles;
                }
            }
        }
    }

    public Set<Player> getWoolCarriers() {
        return woolCarriers;
    }

    public void add(Player player) {
        this.woolCarriers.add(player);
    }

    public void remove(Player player) {
        this.woolCarriers.remove(player);
    }

    public void clear() {
        this.woolCarriers.clear();
    }
}
