package tc.oc.pgm.command;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.command.util.ParserConstants.CURRENT;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotation.specifier.Greedy;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Default;
import org.incendo.cloud.annotations.Flag;
import org.incendo.cloud.annotations.Permission;
import tc.oc.pgm.action.ActionMatchModule;
import tc.oc.pgm.action.actions.ExposedAction;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.PrettyPaginatedComponentResults;
import tc.oc.pgm.util.text.TextFormatter;

@Command("action|actions")
public class ActionCommand {

  @Command("[page]")
  @CommandDescription("List available exposed actions")
  @Permission(Permissions.GAMEPLAY)
  public void fallback(
      Audience audience,
      CommandSender sender,
      ActionMatchModule amm,
      @Argument("page") @Default("1") int page,
      @Flag(value = "query", aliases = "q") String query,
      @Flag(value = "all", aliases = "a") boolean all) {
    list(audience, sender, amm, page, query, all);
  }

  @Command("list|page [page]")
  @CommandDescription("List available exposed actions")
  @Permission(Permissions.GAMEPLAY)
  public void list(
      Audience audience,
      CommandSender sender,
      ActionMatchModule amm,
      @Argument("page") @Default("1") int page,
      @Flag(value = "query", aliases = "q") String query,
      @Flag(value = "all", aliases = "a") boolean all) {

    List<ExposedAction> actions =
        amm.getExposedActions().stream()
            .filter(a -> query == null || a.getId().contains(query))
            .sorted(Comparator.comparing(ExposedAction::getId))
            .collect(Collectors.toList());

    int resultsPerPage = all ? actions.size() : 8;
    int pages = all ? 1 : (actions.size() + resultsPerPage - 1) / resultsPerPage;

    Component title =
        TextFormatter.paginate(
            text("Actions"), page, pages, NamedTextColor.DARK_AQUA, NamedTextColor.AQUA, true);
    Component header = TextFormatter.horizontalLineHeading(sender, title, NamedTextColor.BLUE);

    PrettyPaginatedComponentResults.display(
        audience,
        actions,
        page,
        resultsPerPage,
        header,
        (v, i) -> text((i + 1) + ". ").append(text(v.getId(), NamedTextColor.AQUA)));
  }

  @Command("trigger <action> [target]")
  @CommandDescription("Trigger a specific action")
  @Permission(Permissions.GAMEPLAY)
  public <T extends Filterable<?>> void triggerAction(
      Audience audience,
      @Argument("action") @Greedy ExposedAction action,
      @Argument("target") @Default(CURRENT) MatchPlayer target) {
    action.trigger(target);
    audience.sendMessage(text("Triggered " + action.getId()));
  }

  @Command("untrigger <action> [target]")
  @CommandDescription("Untrigger a specific action")
  @Permission(Permissions.GAMEPLAY)
  public <T extends Filterable<?>> void untriggerAction(
      Audience audience,
      @Argument("action") @Greedy ExposedAction action,
      @Argument("target") @Default(CURRENT) MatchPlayer target) {
    action.untrigger(target);
    audience.sendMessage(text("Untriggered " + action.getId()));
  }
}
