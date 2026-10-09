package eu.koolfreedom.util;

import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.player.AltManager;
import eu.koolfreedom.bridge.GroupManagement;
import eu.koolfreedom.config.ConfigEntry;
import eu.koolfreedom.event.AdminChatEvent;
import eu.koolfreedom.event.PublicBroadcastEvent;
import lombok.Getter;
import lombok.NonNull;
import net.kyori.adventure.key.Namespaced;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Modifying;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class FUtil // the f stands for fuck
{
    private static final Random RANDOM = new Random();
    private static final MiniMessage MINI_MESSAGE = MiniMessage.builder().tags(TagResolver.builder()
            .resolver(StandardTags.defaults())
            .resolver(TagResolver.resolver("randomize", RandomColorTag.randomColorTag))
            .build()).build();
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    /**
     * Broadcasts a staff action to the server. This calls the {@link PublicBroadcastEvent}.
     * @param sender        The {@link CommandSender} performing the action.
     * @param message       The message being broadcast as a String in MiniMessage format.
     * @param placeholders  An array of {@link TagResolver} instances. If you don't want to have any additional
     *                      placeholders, just leave out that argument.
     */
    public static void staffAction(@NonNull CommandSender sender, @NonNull String message,
                                   @NonNull TagResolver... placeholders)
    {
        broadcast("<gray><i>[<sender>: <action>]", Placeholder.unparsed("sender", sender.getName()),
                Placeholder.component("action", FUtil.miniMessage(message, placeholders)));
    }

    /**
     * Broadcasts a text component to the server. This calls the {@link PublicBroadcastEvent}.
     * @param component     The {@link Component} being broadcasted.
     */
    public static void broadcast(Component component)
    {
        broadcast(true, component);
    }

    /**
     * Broadcasts a text component to the server. This can optionally call the {@link PublicBroadcastEvent}.
     * @param callEvent     Whether to call the {@link PublicBroadcastEvent}.
     * @param component     The {@link Component} being broadcasted.
     */
    public static void broadcast(boolean callEvent, Component component)
    {
        Bukkit.broadcast(component);

        if (callEvent)
        {
            new PublicBroadcastEvent(!Bukkit.isPrimaryThread(), component).callEvent();
        }
    }

    /**
     * Broadcasts a text component to users on the server with a specific permission node. This does not call the
     *  {@link PublicBroadcastEvent}.
     * @param component     The {@link Component} being broadcasted.
     * @param permission    The permission node required for a player to see the message.
     */
    public static void broadcast(Component component, String permission)
    {
        Bukkit.broadcast(component, permission);
    }

    /**
     * Broadcasts a MiniMessage-formatted message to the server. This calls the {@link PublicBroadcastEvent}.
     * @param message       The message being broadcast as a String in MiniMessage format.
     * @param placeholders  An array of {@link TagResolver} instances. If you don't want to have any additional
     *                      placeholders, just leave out that argument.
     */
    public static void broadcast(String message, TagResolver... placeholders)
    {
        broadcast(true, message, placeholders);
    }

    /**
     * Broadcasts a MiniMessage-formatted message to the server. This can optionally call the
     *  {@link PublicBroadcastEvent}.
     * @param callEvent     Whether to call the {@link PublicBroadcastEvent}.
     * @param message       The message being broadcast as a String in MiniMessage format.
     * @param placeholders  An array of {@link TagResolver} instances. If you don't want to have any additional
     *                      placeholders, just leave out that argument.
     */
    public static void broadcast(boolean callEvent, String message, TagResolver... placeholders)
    {
        Bukkit.broadcast(miniMessage(message, placeholders));

        if (callEvent)
        {
            new PublicBroadcastEvent(!Bukkit.isPrimaryThread(), miniMessage(message, placeholders)).callEvent();
        }
    }

    /**
     * Broadcasts a MiniMessage-formatted message to users on the server with a specific permission node. This does not
     *  call the {@link PublicBroadcastEvent}.
     * @param permission    The permission node required for a player to see the message.
     * @param message       The message being broadcast as a String in MiniMessage format.
     * @param placeholders  An array of {@link TagResolver} instances. If you don't want to have any additional
     *                      placeholders, just leave out that argument.
     */
    public static void broadcast(String permission, String message, TagResolver... placeholders)
    {
        Bukkit.broadcast(miniMessage(message, placeholders), permission);
    }

    /**
     * Processes MiniMessage messages into a regular Adventure text component.
     * @param message       The message as a String in MiniMessage format.
     * @param placeholders  An array of {@link TagResolver} instances. If you don't want to have any additional
     *                      placeholders, just leave out that argument.
     * @return              The resulting {@link Component}.
     */
    public static Component miniMessage(String message, TagResolver... placeholders)
    {
        return MINI_MESSAGE.deserialize(message, placeholders);
    }

    /**
     * Get the content of a component as a plain text String.
     * @param input     The {@link Component} to get as a component.
     * @return          The resulting String.
     */
    public static String plainText(Component input)
    {
        return PLAIN_TEXT.serialize(input);
    }

    /**
     * Get a random number between two numbers.
     * @param min   The lowest the number can be.
     * @param max   The highest the number can be.
     * @return      A randomly generated number.
     */
    public static int randomNumber(int min, int max)
    {
        if (min > max)
        {
            throw new IllegalArgumentException("Numbers are flipped. Max < min.");
        }

        return RANDOM.nextInt(min, max);
    }

    /**
     * Get a randomly generated String that is the given length.
     * @param length    int
     * @return          A randomly generated alphanumeric string.
     */
    public static String randomString(int length)
    {
        return RANDOM.ints(48, 122)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
    }

    /**
     * Sends a message to the admin chat on all platforms asynchronously.
     * @see                 #adminChat(boolean, CommandSender, Component, String, GroupManagement.Group, Component, Namespaced)
     * @param senderDisplay The sender's display name, represented as a {@link Component}.
     * @param senderName    The sender's name.
     * @param group         The {@link eu.koolfreedom.bridge.GroupManagement.Group} the sender is in.
     * @param message       The message sent to the chat, represented as a {@link Component}.
     * @param caller        An instance of {@link Namespaced}, such as {@link org.bukkit.NamespacedKey}. This is
     *                      mandatory, as it is used to check if a particular event was sent by a given source and
     *                      prevent duplicate or even endless messages from being sent.
     */
    public static void asyncAdminChat(Component senderDisplay, String senderName, GroupManagement.Group group,
                                      Component message, Namespaced caller)
    {
        adminChat(true, null, senderDisplay, senderName, group, message, caller);
    }

    /**
     * Sends a message to the admin chat on all platforms asynchronously.
     * @see                 #adminChat(boolean, CommandSender, Component, String, GroupManagement.Group, Component, Namespaced)
     * @param sender        The {@link CommandSender} responsible for the message.
     * @param group         The {@link eu.koolfreedom.bridge.GroupManagement.Group} the sender is in.
     * @param message       The message sent to the chat, represented as a {@link Component}.
     * @param caller        An instance of {@link Namespaced}, such as {@link org.bukkit.NamespacedKey}. This is
     *                      mandatory, as it is used to check if a particular event was sent by a given source and
     *                      prevent duplicate or even endless messages from being sent.
     */
    public static void asyncAdminChat(CommandSender sender, GroupManagement.Group group, Component message,
                                      Namespaced caller)
    {
        adminChat(true, sender, sender instanceof Player player ? player.displayName() :
                Component.text(sender.getName()), sender.getName(), group, message, caller);
    }

    /**
     * Sends a message to the admin chat on all platforms synchronously.
     * @see                 #adminChat(boolean, CommandSender, Component, String, GroupManagement.Group, Component, Namespaced)
     * @param sender        The {@link CommandSender} responsible for the message.
     * @param group         The {@link eu.koolfreedom.bridge.GroupManagement.Group} the sender is in.
     * @param message       The message sent to the chat, represented as a {@link Component}.
     * @param caller        An instance of {@link Namespaced}, such as {@link org.bukkit.NamespacedKey}. This is
     *                      mandatory, as it is used to check if a particular event was sent by a given source and
     *                      prevent duplicate or even endless messages from being sent.
     */
    public static void adminChat(CommandSender sender, GroupManagement.Group group, Component message, Namespaced caller)
    {
        adminChat(false, sender, sender instanceof Player player ? player.displayName() :
                Component.text(sender.getName()), sender.getName(), group, message, caller);
    }

    /**
     * Sends a message to the admin chat on all platforms synchronously.
     * @see                 #adminChat(boolean, CommandSender, Component, String, GroupManagement.Group, Component, Namespaced)
     * @param senderDisplay The sender's display name, represented as a {@link Component}.
     * @param senderName    The sender's name.
     * @param group         The {@link eu.koolfreedom.bridge.GroupManagement.Group} the sender is in.
     * @param message       The message sent to the chat, represented as a {@link Component}.
     * @param caller        An instance of {@link Namespaced}, such as {@link org.bukkit.NamespacedKey}. This is
     *                      mandatory, as it is used to check if a particular event was sent by a given source and
     *                      prevent duplicate or even endless messages from being sent.
     */
    public static void adminChat(@NonNull Component senderDisplay, @NonNull String senderName,
                                 @NonNull GroupManagement.Group group, @NonNull Component message,
                                 @NonNull Namespaced caller)
    {
        adminChat(false, null, senderDisplay, senderName, group, message, caller);
    }

    /**
     * Sends a message to the admin chat on all platforms by calling the {@link AdminChatEvent} event and broadcasting
     *  the message in-game.
     * @param async         Whether to call the event asynchronously. This is required to be true to send admin chat
     *                      events from platforms like Discord.
     * @param sender        The {@link CommandSender} behind a given message. This can be null if {@code senderDisplay}
     *                      and {@code senderName} are not null.
     * @param senderDisplay The sender's display name, represented as a {@link Component}. This can be null if
     *                      {@code sender} is not null.
     * @param senderName    The sender's name. This can be null if {@code sender} is not null.
     * @param group         The {@link eu.koolfreedom.bridge.GroupManagement.Group} the sender is in.
     * @param message       The message sent to the chat, represented as a {@link Component}.
     * @param caller        An instance of {@link Namespaced}, such as {@link org.bukkit.NamespacedKey}. This is
     *                      mandatory, as it is used to check if a particular event was sent by a given source and
     *                      prevent duplicate or even endless messages from being sent.
     */
    private static void adminChat(boolean async, @Nullable CommandSender sender, @Nullable Component senderDisplay,
                                  @Nullable String senderName, @NonNull GroupManagement.Group group,
                                  @NonNull Component message, @NonNull Namespaced caller)
    {
        // Mandates having one or the other to avoid possible issues in the future
        if (sender == null)
        {
            Objects.requireNonNull(senderName);
            Objects.requireNonNull(senderDisplay);
        }
        else if (senderName == null && senderDisplay == null)
        {
            Objects.requireNonNull(sender);
        }

        Component formattedMessage = miniMessage(ConfigEntry.FORMATS_ADMIN_CHAT.getString(),
                Placeholder.unparsed("name", sender != null ? sender.getName() : senderName),
                Placeholder.component("display_name", senderDisplay != null ?
                        senderDisplay :
                        sender instanceof Player player ?
                                player.displayName() :
                                Component.text(sender.getName())),
                Placeholder.component("rank", group.getDisplayName()),
                Placeholder.styling("rank_color", rank -> rank.color(group.getColor())),
                Placeholder.component("message", message));

        broadcast(formattedMessage, "kfc.command.adminchat");
        new AdminChatEvent(async, sender, senderDisplay, senderName, group, message, caller).callEvent();
    }

    /**
     * Get a player's IP address.
     * @param player    The {@link Player} in question.
     * @return          The player's IP address.
     */
    public static String getIp(@NonNull Player player)
    {
        return Objects.requireNonNull(player.getAddress()).getAddress().getHostAddress().trim();
    }

    /**
     * Get a list of the names of all players currently online, plus or minus the users who are vanished.
     * @param includeVanish Whether to include vanished players or not.
     * @return              A list of player names which includes or excludes vanished players (depending on the given
     *                      parameters).
     */
    public static List<String> getPlayerList(boolean includeVanish)
    {
        return Bukkit.getOnlinePlayers().stream().filter(player ->
                KoolSMPCore.getInstance().getVanishBridge() != null &&
                        KoolSMPCore.getInstance().getVanishBridge().isVanished(player) || includeVanish)
                .map(Player::getName).toList();
    }

    /**
     * Fetches a player currently online unless they are vanished and the given parameter does not allow it.
     * @param name          The player's username
     * @param seeVanished   Whether to return the {@link Player} or not.
     * @return              The {@link Player}. This returns null if the player is not online, or they are vanished and
     *                      {@code seeVanished} is false.
     */
    public static Player getPlayer(String name, boolean seeVanished)
    {
        Player player = Bukkit.getPlayer(name);
        return seeVanished || KoolSMPCore.getInstance().getVanishBridge() != null
                && !KoolSMPCore.getInstance().getVanishBridge().isVanished(player) ? player : null;
    }

    public static class RandomColorTag implements Modifying
    {
        @Getter
        private static final RandomColorTag randomColorTag = new RandomColorTag();

        @Override
        public Component apply(@NotNull Component current, int depth)
        {
            if (current instanceof TextComponent textComponent)
            {
                return Component.join(JoinConfiguration.spaces(), Arrays.stream(textComponent.content().split(" "))
                        .map(text -> Component.text(text).colorIfAbsent(TextColor.color(randomNumber(0, 255),
                                randomNumber(0, 255), randomNumber(0, 255)))).toList());
            }

            return current;
        }
    }

    public static boolean isValidIP(String ip)
    {
        if (ip == null)
        {
            return false;
        }

        return ip.matches(
                "^((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)(\\.|$)){4}$"
        );
    }

    public static List<OfflinePlayer> getOfflinePlayersByIp(String ip) {
        AltManager altManager = KoolSMPCore.getInstance().getAltManager();
        Set<UUID> uuids = altManager.getAlts(ip);

        List<OfflinePlayer> players = new ArrayList<>();
        for (UUID uuid : uuids) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            players.add(offlinePlayer);
        }

        return players;
    }
}
