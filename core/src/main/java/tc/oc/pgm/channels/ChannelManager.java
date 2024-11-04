package tc.oc.pgm.channels;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.text.TextException.exception;
import static tc.oc.pgm.util.text.TextException.noPermission;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChatTabCompleteEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.channels.Channel;
import tc.oc.pgm.api.event.ChannelMessageEvent;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.api.setting.Settings;
import tc.oc.pgm.ffa.Tribute;
import tc.oc.pgm.util.Players;
import tc.oc.pgm.util.bukkit.OnlinePlayerUUIDMapAdapter;
import tc.oc.pgm.util.text.TextException;

public class ChannelManager implements Listener {

  private static final Cache<AsyncPlayerChatEvent, Boolean> CHAT_EVENT_CACHE =
      CacheBuilder.newBuilder()
          .weakKeys()
          .expireAfterWrite(15, TimeUnit.SECONDS)
          .build();

  private final GlobalChannel globalChannel;
  private final AdminChannel adminChannel;
  private final TeamChannel teamChannel;
  private final Set<Channel<?>> channels;
  private final Map<Character, Channel<?>> shortcuts;
  private final OnlinePlayerUUIDMapAdapter<Channel<?>> selectedChannel;

  private CommandManager<CommandSender> manager;

  public ChannelManager() {
    this.channels = new HashSet<>();
    this.channels.add(globalChannel = new GlobalChannel());
    this.channels.add(adminChannel = new AdminChannel());
    this.channels.add(teamChannel = new TeamChannel());
    this.channels.add(new PrivateMessageChannel());
    this.channels.addAll(Integration.pollRegisteredChannels());

    this.shortcuts = new HashMap<>();
    for (Channel<?> channel : channels) {
      if (channel.getShortcut() == null) continue;

      this.shortcuts.putIfAbsent(channel.getShortcut(), channel);
    }

    this.selectedChannel = new OnlinePlayerUUIDMapAdapter<>(PGM.get());
  }

  public void registerCommands(CommandManager<CommandSender> manager) {
    this.manager = manager;

    for (Channel<?> channel : PGM.get().getChannelManager().getChannels()) {
      channel.registerCommand(manager);
    }
  }

  public void processChat(MatchPlayer sender, String message) {
    if (message.isEmpty()) return;

    CommandContext<CommandSender> context = new CommandContext<>(sender.getBukkit(), manager);
    Channel<?> channel = shortcuts.get(message.charAt(0));

    if (channel != null && channel.canSendMessage(sender)) {
      channel.processChatShortcut(sender, message, context);
    }

    if (channel == null) {
      channel = getSelectedChannel(sender);
      channel.processChatMessage(sender, message, context);
    }

    if (context.contains(Channel.MESSAGE_KEY)) process(channel, sender, context);
  }

  public void process(
      Channel<?> channel, MatchPlayer sender, CommandContext<CommandSender> context) {
    processChannelMessage(calculateChannelRedirect(channel, sender, context), sender, context);
  }

  private <T> void processChannelMessage(
      Channel<T> channel, MatchPlayer sender, CommandContext<CommandSender> context) {
    if (!channel.canSendMessage(sender)) throw noPermission();
    throwMuted(sender);

    T target = channel.getTarget(sender, context);
    Collection<MatchPlayer> viewers = channel.getViewers(target);

    final AsyncPlayerChatEvent asyncEvent = new AsyncPlayerChatEvent(
        false,
        sender.getBukkit(),
        context.get(Channel.MESSAGE_KEY),
        viewers.stream().map(MatchPlayer::getBukkit).collect(Collectors.toSet()));

    CHAT_EVENT_CACHE.put(asyncEvent, true);
    sender.getMatch().callEvent(asyncEvent);
    if (asyncEvent.isCancelled()) return;

    final ChannelMessageEvent<T> event =
        new ChannelMessageEvent<>(channel, sender, target, viewers, asyncEvent.getMessage());

    sender.getMatch().callEvent(event);

    if (event.isCancelled()) {
      if (event.getSender() != null && event.getCancellationReason() != null) {
        event.getSender().sendWarning(event.getCancellationReason());
      }
      return;
    }

    Component finalMessage = event
        .getChannel()
        .formatMessage(event.getTarget(), event.getSender(), event.getComponent());
    event.getViewers().forEach(player -> player.sendMessage(finalMessage));

    channel.messageSent(event);
  }

  private Channel<?> calculateChannelRedirect(
      Channel<?> channel, MatchPlayer sender, CommandContext<CommandSender> context) {
    if (Integration.isVanished(sender.getBukkit()) && !(channel instanceof AdminChannel)) {
      // Allow private messaging with players who can see each other
      if (channel instanceof PrivateMessageChannel pmc && pmc.canSendVanished(sender, context)) {
        return channel;
      }

      if (!channel.supportsRedirect()) throw exception("vanish.chat.deny");

      return adminChannel;
    }

    // Try to use global chat when a match has ended
    if (channel.supportsRedirect()
        && (sender.getMatch().isFinished() || sender.getParty() instanceof Tribute)) {
      if (channel instanceof TeamChannel) return globalChannel;
    }

    return channel;
  }

  private void throwMuted(MatchPlayer player) {
    if (!Integration.isMuted(player.getBukkit())) return;
    Optional<String> muteReason =
        Optional.ofNullable(Integration.getMuteReason(player.getBukkit()));
    Component reason =
        muteReason.isPresent() ? text(muteReason.get()) : translatable("moderation.mute.noReason");

    throw exception("moderation.mute.message", reason.color(NamedTextColor.AQUA));
  }

  @EventHandler
  public void onPlayerTabComplete(PlayerChatTabCompleteEvent event) {
    if (event.getChatMessage().trim().equals(event.getLastToken())) {
      char first = event.getLastToken().charAt(0);
      if (shortcuts.containsKey(first)) {
        List<String> suggestions =
            Players.getPlayerNames(event.getPlayer(), event.getLastToken().substring(1));
        suggestions.replaceAll(s -> first + s);

        event.getTabCompletions().addAll(suggestions);
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerJoin(final PlayerJoinEvent event) {
    MatchPlayer player = PGM.get().getMatchManager().getPlayer(event.getPlayer());
    if (player == null) return;
    selectedChannel.put(
        player.getId(), findChannelBySetting(player.getSettings().getValue(SettingKey.CHAT)));
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onChat(AsyncPlayerChatEvent event) {
    if (CHAT_EVENT_CACHE.getIfPresent(event) != null) {
      // PGM created chat event, ignore it
      CHAT_EVENT_CACHE.invalidate(event);
      return;
    }

   event.setCancelled(true);

    final MatchPlayer player = PGM.get().getMatchManager().getPlayer(event.getPlayer());
    if (player == null) return;

    final String message = event.getMessage().trim();
    try {
      processChat(player, message);
    } catch (TextException e) {
      // Allow sub-handlers to throw command exceptions just fine
      player.sendWarning(e);
    }
  }

  private Channel<?> findChannelBySetting(SettingValue setting) {
    for (Channel<?> channel : channels) {
      if (setting == channel.getSetting()) return channel;
    }

    return teamChannel;
  }

  public void setChannel(MatchPlayer player, SettingValue value) {
    selectedChannel.put(player.getId(), findChannelBySetting(value));
  }

  public void setChannel(MatchPlayer player, Channel<?> channel) {
    Channel<?> previous = selectedChannel.get(player.getId());
    selectedChannel.put(player.getId(), channel);

    if (channel.getSetting() != null) {
      Settings setting = player.getSettings();
      final SettingValue old = setting.getValue(SettingKey.CHAT);

      if (old != channel.getSetting()) {
        setting.setValue(SettingKey.CHAT, channel.getSetting());
      }
    }

    if (previous != channel) {
      player.sendMessage(translatable(
          "setting.set",
          text("chat"),
          text(previous.getDisplayName(), NamedTextColor.GRAY),
          text(channel.getDisplayName(), NamedTextColor.GREEN)));
    } else {
      player.sendMessage(translatable(
          "setting.get", text("chat"), text(previous.getDisplayName(), NamedTextColor.GREEN)));
    }
  }

  public Channel<?> getSelectedChannel(MatchPlayer player) {
    return selectedChannel.getOrDefault(player.getId(), globalChannel);
  }

  public Set<Channel<?>> getChannels() {
    return channels;
  }

  public AdminChannel getAdminChannel() {
    return adminChannel;
  }

  public static void broadcastMessage(Component message) {
    PGM.get().getChannelManager().globalChannel.broadcastMessage(message, null);
  }

  public static void broadcastMessage(Component message, Predicate<MatchPlayer> filter) {
    PGM.get().getChannelManager().globalChannel.broadcastMessage(message, null, filter);
  }

  public static void broadcastAdminMessage(Component message) {
    PGM.get().getChannelManager().adminChannel.broadcastMessage(message, null);
  }

  public static void broadcastPartyMessage(Component message, Party party) {
    PGM.get().getChannelManager().teamChannel.broadcastMessage(message, party);
  }

  public static void broadcastPartyMessage(
      Component message, Party party, Predicate<MatchPlayer> filter) {
    PGM.get().getChannelManager().teamChannel.broadcastMessage(message, party, filter);
  }
}
