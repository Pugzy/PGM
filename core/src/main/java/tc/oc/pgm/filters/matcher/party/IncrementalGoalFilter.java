package tc.oc.pgm.filters.matcher.party;

import org.bukkit.event.Event;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.filter.query.MatchQuery;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.goals.Goal;
import tc.oc.pgm.goals.GoalDefinition;
import tc.oc.pgm.goals.IncrementalGoal;
import tc.oc.pgm.goals.events.GoalCompleteEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

/**
 * Matches teams/players who have completed the given objective. If a team is given, then the filter
 * matches whenever that team has completed the objective, regardless of what is passed to the
 * query. If the anyTeam flag is set, then the filter matches when any team has completed the
 * objective.
 */
public class IncrementalGoalFilter implements CompetitorFilter {
  private final FeatureReference<? extends GoalDefinition> goal;

  public IncrementalGoalFilter(FeatureReference<? extends GoalDefinition> goal) {
    this.goal = goal;
  }

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return Collections.singleton(GoalCompleteEvent.class);
  }

  public boolean matches(MatchQuery query, Optional<Competitor> competitor) {
    Goal<? extends GoalDefinition> matchGoal = this.goal.get().getGoal(query.getMatch());

    if (matchGoal instanceof IncrementalGoal) {
      return ((IncrementalGoal<? extends GoalDefinition>) matchGoal).getCompletion() > 0;
    }

    return matchGoal.isCompleted(competitor);
  }

  @Override
  public boolean matchesAny(MatchQuery query) {
    return matches(query, Optional.empty());
  }

  @Override
  public boolean matches(MatchQuery query, Competitor competitor) {
    return matches(query, Optional.of(competitor));
  }
}
