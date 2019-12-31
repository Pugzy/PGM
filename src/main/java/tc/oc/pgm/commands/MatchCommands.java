package tc.oc.pgm.commands;

import app.ashcon.intake.Command;
import app.ashcon.intake.parametric.annotation.Default;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.AllTranslations;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.chat.Audience;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.match.MatchPhase;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.commands.annotations.Text;
import tc.oc.pgm.ffa.FreeForAllMatchModule;
import tc.oc.pgm.goals.Goal;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.instance.InstanceManager;
import tc.oc.pgm.map.PGMMap;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamMatchModule;
import tc.oc.pgm.util.ComponentPaginatedResult;
import tc.oc.util.StringUtils;
import tc.oc.util.components.PeriodFormats;
import tc.oc.util.localization.Locales;

public class MatchCommands {

  @Command(
      aliases = {"matchinfo", "match"},
      desc = "Shows information about the current match")
  public static void matchInfo(CommandSender sender, Match match) {
    // indicates whether we have game information from the match yet
    boolean haveGameInfo =
        match.getPhase() == MatchPhase.RUNNING || match.getPhase() == MatchPhase.FINISHED;

    sender.sendMessage(
        StringUtils.dashedChatMessage(
            ChatColor.DARK_AQUA
                + " "
                + AllTranslations.get().translate("command.match.matchInfo.title", sender),
            ChatColor.STRIKETHROUGH + "-",
            ChatColor.RED.toString()));
    if (haveGameInfo) {
      // show match time
      sender.sendMessage(
          ChatColor.DARK_PURPLE
              + AllTranslations.get().translate("command.match.matchInfo.time", sender)
              + ": "
              + ChatColor.GOLD
              + PeriodFormats.COLONS_PRECISE
                  .withLocale(Locales.getLocale(sender))
                  .print(match.getDuration().toPeriod()));
    }

    TeamMatchModule tmm = match.getMatchModule(TeamMatchModule.class);
    FreeForAllMatchModule ffamm = match.getMatchModule(FreeForAllMatchModule.class);
    List<String> teamCountParts = Lists.newArrayList();

    if (tmm != null) {
      for (Team team : tmm.getTeams()) {
        StringBuilder msg = new StringBuilder();

        String teamName = team.getName();
        if (teamName.endsWith(" Team")) teamName = teamName.substring(0, teamName.length() - 5);

        msg.append(team.getColor())
            .append(teamName)
            .append(ChatColor.GRAY)
            .append(": ")
            .append(ChatColor.WHITE)
            .append(team.getPlayers().size());

        if (team.getMaxPlayers() != Integer.MAX_VALUE) {
          msg.append(ChatColor.GRAY).append("/").append(team.getMaxPlayers());
        }

        teamCountParts.add(msg.toString());
      }
    } else if (ffamm != null) {
      teamCountParts.add(
          ChatColor.YELLOW
              + AllTranslations.get().translate("command.match.matchInfo.players", sender)
              + ChatColor.GRAY
              + ": "
              + ChatColor.WHITE
              + match.getParticipants().size()
              + ChatColor.GRAY
              + '/'
              + ffamm.getMaxPlayers());
    }

    teamCountParts.add(
        ChatColor.AQUA
            + AllTranslations.get().translate("command.match.matchInfo.observers", sender)
            + ChatColor.GRAY
            + ": "
            + ChatColor.WHITE
            + match.getObservers().size());

    sender.sendMessage(Joiner.on(ChatColor.DARK_GRAY + " | ").join(teamCountParts));

    GoalMatchModule gmm = match.getMatchModule(GoalMatchModule.class);
    if (haveGameInfo && gmm != null) {
      if (tmm != null && gmm.getGoalsByCompetitor().size() > 0) {
        Multimap<Team, String> teamGoalTexts = HashMultimap.create();

        for (Team team : tmm.getParticipatingTeams()) {
          for (Goal goal : gmm.getGoals(team)) {
            if (goal.isVisible()) {
              teamGoalTexts.put(
                  team,
                  (goal.isCompleted(team) ? ChatColor.GREEN : ChatColor.DARK_RED) + goal.getName());
            }
          }
        }

        if (!teamGoalTexts.isEmpty()) {
          sender.sendMessage(
              ChatColor.DARK_PURPLE
                  + AllTranslations.get().translate("command.match.matchInfo.goals", sender)
                  + ":");

          for (Map.Entry<Team, Collection<String>> entry : teamGoalTexts.asMap().entrySet()) {
            Team team = entry.getKey();
            Collection<String> goalTexts = entry.getValue();

            sender.sendMessage(
                "  "
                    + team.getColoredName()
                    + ChatColor.GRAY
                    + ": "
                    + Joiner.on("  ").join(goalTexts));
          }
        }
      } else {
        // FIXME: this is not the best way to handle scores
        ScoreMatchModule smm = match.getMatchModule(ScoreMatchModule.class);
        if (smm != null) {
          sender.sendMessage(smm.getStatusMessage());
        }
      }
    }
  }

  @Command(
      aliases = {"loadnewmatch"},
      desc = "Loads a new match")
  public static void loadNewMatch(
      MatchPlayer player, MatchManager matchManager, Match match, @Default("next") @Text PGMMap map)
      throws Throwable {
    map.reload(true);

    Match newMatch = matchManager.createMatch(null, map);

    teleportMatchPlayer(player, player.getMatch(), newMatch);
  }

  @Command(
      aliases = {"unloadmatch"},
      desc = "Unloads a specified match")
  public static void unloadMatch(
      MatchPlayer player, MatchManager matchManager, Match match, String matchID) throws Throwable {
    matchManager.unloadMatch(matchID);
  }

  @Command(
      aliases = {"matches"},
      desc = "Lists all matches")
  public static void matches(
      Audience audience,
      CommandSender sender,
      Match match,
      MatchManager matchManager,
      @Default("1") int page)
      throws Throwable {

    Collection<Match> matches = matchManager.getMatches();
    int resultsPerPage = 8;
    int pages = (matches.size() + resultsPerPage - 1) / resultsPerPage;

    String listHeader =
        net.md_5.bungee.api.ChatColor.BLUE.toString()
            + net.md_5.bungee.api.ChatColor.STRIKETHROUGH
            + "-----------"
            + net.md_5.bungee.api.ChatColor.RESET
            + " "
            + "Loaded Matches"
            + net.md_5.bungee.api.ChatColor.DARK_AQUA
            + " ("
            + net.md_5.bungee.api.ChatColor.AQUA
            + page
            + net.md_5.bungee.api.ChatColor.DARK_AQUA
            + " of "
            + net.md_5.bungee.api.ChatColor.AQUA
            + pages
            + net.md_5.bungee.api.ChatColor.DARK_AQUA
            + ") "
            + net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', "&9&m-----------");

    new ComponentPaginatedResult<Match>(listHeader, resultsPerPage) {
      @Override
      public Component format(Match match, int index) {

        PGMMap map = match.getMap();

        String arrow =
            match.getPhase().equals(MatchPhase.RUNNING)
                ? net.md_5.bungee.api.ChatColor.GREEN + "» "
                : "» ";

        Component rendered = new PersonalizedText("Match #" + match.getId());
        rendered.color(net.md_5.bungee.api.ChatColor.GRAY);
        BaseComponent dupe = rendered.duplicate();

        rendered.clickEvent(
            new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpmatch " + match.getId()));
        rendered.hoverEvent(
            HoverEvent.Action.SHOW_TEXT,
            new PersonalizedTranslatable("tip.teleportTo", dupe).render());

        return new PersonalizedText(
            new PersonalizedText(
                arrow
                    + net.md_5.bungee.api.ChatColor.GOLD
                    + map.getName()
                    + net.md_5.bungee.api.ChatColor.DARK_AQUA
                    + " ("),
            rendered,
            new PersonalizedText(
                net.md_5.bungee.api.ChatColor.AQUA
                    + " Players: "
                    + net.md_5.bungee.api.ChatColor.WHITE
                    + match.getPlayers().size()
                    + net.md_5.bungee.api.ChatColor.DARK_AQUA
                    + ")"));
      }
    }.display(audience, matches, page);
  }

  @Command(
      aliases = {"tpmatch"},
      desc = "Teleports to specified match number")
  public static void tpMatch(MatchPlayer player, Match match, String matchID) throws Throwable {
    Collection<Match> matches = PGM.get().getMatchManager().getMatches();

    for (Match runningMatch : matches) {
      if (runningMatch.getId().equals(matchID)) {
        teleportMatchPlayer(player, player.getMatch(), runningMatch);
        break;
      }
    }
  }

  @Command(
      aliases = {"managerreload"},
      desc = "Teleports to specified match number")
  public static void managerReload(MatchPlayer player, Match match) {
    InstanceManager.get().loadInstances();
  }

  private static void teleportMatchPlayer(MatchPlayer matchPlayer, Match oldMatch, Match newMatch) {
    oldMatch.removePlayer(matchPlayer.getBukkit());
    newMatch.addPlayer(matchPlayer.getBukkit());
  }
}
