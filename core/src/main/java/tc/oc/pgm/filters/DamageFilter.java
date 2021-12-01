package tc.oc.pgm.filters;

import com.google.common.collect.Range;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.blitz.BlitzMatchModule;

public class DamageFilter extends ParticipantFilter {
  private final Range<Double> range;
  private final Integer count;

  public DamageFilter(Range<Double> range, Integer count) {
    this.range = range;
    this.count = count;
  }

  @Override
  protected QueryResponse queryPlayer(PlayerQuery query, MatchPlayer player) {

    int lives = player.getMatch().needModule(BlitzMatchModule.class).getNumOfLives(player.getId());


    //    int streak =
    //
    // player.getMatch().needModule(KillRewardMatchModule.class).getKillStreak(player.getId());
    //    if (this.repeat && streak > 0) {
    //      int modulo =
    //          this.range.upperEndpoint() - (this.range.upperBoundType() == BoundType.CLOSED ? 0 :
    // 1);
    //      streak = 1 + (streak - 1) % modulo;
    //    }
    return QueryResponse.fromBoolean(false); // this.range.contains(streak)
  }
}
