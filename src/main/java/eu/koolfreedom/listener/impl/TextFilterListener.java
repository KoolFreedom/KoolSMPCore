package eu.koolfreedom.listener.impl;

import eu.koolfreedom.banning.Ban;
import eu.koolfreedom.command.impl.DoomCommand;
import eu.koolfreedom.command.impl.SmiteCommand;
import eu.koolfreedom.config.ConfigEntry;
import eu.koolfreedom.listener.KoolListener;
import eu.koolfreedom.punishment.Punishment;
import eu.koolfreedom.util.FLog;
import eu.koolfreedom.util.FUtil;
import eu.koolfreedom.util.TimeOffset;
import io.papermc.paper.event.player.AsyncChatEvent;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerKickEvent;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class TextFilterListener extends KoolListener
{
    private final Set<TextFilterListener.FilterEntry> filters = new HashSet<>();

    public TextFilterListener()
    {
        loadFilters();
    }

    public void loadFilters()
    {
        if (!filters.isEmpty())
            filters.clear();

        ConfigurationSection section = ConfigEntry.CHAT_FILTER_CATEGORIES.getConfigurationSection();
        section.getKeys(false).stream().filter(section::isConfigurationSection).forEach(key ->
                filters.add(TextFilterListener.FilterEntry.fromConfigurationSection(key, Objects.requireNonNull(section.getConfigurationSection(key)))));
    }

    public Optional<TextFilterListener.FilterEntry> getFilterEntryIfApplicable(OfflinePlayer player, String message)
    {
        return filters.stream().filter(entry -> entry.isApplicable(player, message)).findAny();
    }

    private static void notifyAdmins(Component component)
    {
        for (Player player : Bukkit.getOnlinePlayers())
        {
            if (!player.hasPermission("kfc.admin")) continue;

            player.sendMessage(Component.newline()
                    .append(Component.text("[", NamedTextColor.DARK_GRAY))
                    .append(Component.text("KoolFreedom | Chat Filter", NamedTextColor.RED))
                    .append(Component.text("] ", NamedTextColor.DARK_GRAY))
                    .append(component)
                    .appendNewline());
        }

        FLog.info(component);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    @SuppressWarnings("deprecation")
    private void silentlyCancelDeprecatedChatEventIfCalled(AsyncPlayerChatEvent event)
    {
        // This is here because Paper's developers decided that AsyncChatEvent being cancelled does not influence the
        //  outcome of AsyncPlayerChatEvent. We cancel AsyncChatEvent, but AsyncPlayerChatEvent still gets called, which
        //  is incredibly annoying when you realize that Paper then deprecated APCE, despite you still needing to catch
        //  it. So, you're inevitably going to get warnings about deprecation that are *not* your fault.
        if (ConfigEntry.CHAT_FILTER_ENABLED.getBoolean()) {
            getFilterEntryIfApplicable(event.getPlayer(), event.getMessage()).ifPresent(filter ->
            {
                if (filter.isCancel()) {
                    event.setCancelled(true);
                }
            });
        }
    }

    @EventHandler
    public void onSignEdit(SignChangeEvent event)
    {
        final Player player = event.getPlayer();
        for (Component line : event.lines())
        {
            final String text = FUtil.plainText(line);
            getFilterEntryIfApplicable(player, text).ifPresent(filter ->
            {
                if (filter.isCancel())
                    event.setCancelled(true);

                Bukkit.getScheduler().runTaskLater(plugin, () ->
                {
                    switch (filter.getAction())
                    {
                        case DOOM:
                        {
                            // Only doom them if they are online, otherwise we just fall back to just banning them
                            if (player.isOnline())
                            {
                                DoomCommand.eviscerate(player, Bukkit.getConsoleSender(), filter.getReason());
                                notifyAdmins(mmDeserialize("<red>Player has triggered the sign filter | Content: '<message>'",
                                        Placeholder.unparsed("message", text)));
                                break;
                            }
                        }
                        case SMITE:
                        {
                            if (player.isOnline())
                            {
                                SmiteCommand.zap(player, Bukkit.getConsoleSender(), filter.getReason());
                                notifyAdmins(mmDeserialize("<red>Player has triggered the sign filter | Content: '<message>'",
                                        Placeholder.unparsed("message", text)));
                                break;
                            }
                        }
                        case BAN:
                        {
                            final Ban ban = Ban.fromPlayer(player, "KoolSMPCore Text Filter", filter.getReason(), TimeOffset.getOffset("1d"));
                            plugin.getBanManager().addBan(ban);
                            plugin.getRecordKeeper().recordPunishment(Punishment.fromBan(ban));
                            player.kick(ban.getKickMessage(), PlayerKickEvent.Cause.BANNED);
                            Bukkit.getOnlinePlayers().stream().filter(suspect -> FUtil.getIp(player).equalsIgnoreCase(FUtil.getIp(player))).forEach(suspect ->
                                    player.kick(ban.getKickMessage()));
                            notifyAdmins(mmDeserialize("<red>Player has triggered the sign filter | Content: '<message>'",
                                    Placeholder.unparsed("message", text)));
                            break;
                        }
                        case LOG:
                        {
                            FLog.warning(filter.getReason(), player.getName(), text);
                            notifyAdmins(mmDeserialize("<red>Player has triggered the sign filter | Content: '<message>'",
                                    Placeholder.unparsed("message", text)));
                            break;
                        }
                        case MUTE:
                        {
                            MuteManager muteManager = plugin.getMuteManager();

                            if (!muteManager.isMuted(player))
                            {
                                muteManager.mute(player);
                                FUtil.staffAction(Bukkit.getConsoleSender(), "Muted <player>",
                                        Placeholder.unparsed("player", player.getName()));
                                plugin.getRecordKeeper().recordPunishment(Punishment.builder()
                                        .uuid(player.getUniqueId())
                                        .name(player.getName())
                                        .ip(player.isOnline() ? FUtil.getIp(player) : null)
                                        .by(Bukkit.getConsoleSender().getName())
                                        .type("MUTE")
                                        .build());
                            }
                            notifyAdmins(mmDeserialize("<red>Player has triggered the command filter | Content: '<message>'",
                                    Placeholder.unparsed("message", text)));
                        }
                        default:
                        {
                            // Do nothing
                        }
                    }
                }, filter.getDelay());
            });
        }
    }

    @EventHandler
    public void onPlayerBookEdit(PlayerEditBookEvent event)
    {
        final Player player = event.getPlayer();
        for (Component page : event.getNewBookMeta().pages())
        {
            final String text = FUtil.plainText(page);
            getFilterEntryIfApplicable(player, text).ifPresent(filter ->
            {
                if (filter.isCancel())
                    event.setCancelled(true);

                Bukkit.getScheduler().runTaskLater(plugin, () ->
                {
                    switch (filter.getAction())
                    {
                        case DOOM:
                        {
                            // Only doom them if they are online, otherwise we just fall back to just banning them
                            if (player.isOnline())
                            {
                                DoomCommand.eviscerate(player, Bukkit.getConsoleSender(), filter.getReason());
                                notifyAdmins(mmDeserialize("<red>Player has triggered the book filter | Content: '<message>'",
                                        Placeholder.unparsed("message", text)));
                                break;
                            }
                        }
                        case SMITE:
                        {
                            if (player.isOnline())
                            {
                                SmiteCommand.zap(player, Bukkit.getConsoleSender(), filter.getReason());
                                notifyAdmins(mmDeserialize("<red>Player has triggered the book filter | Content: '<message>'",
                                        Placeholder.unparsed("message", text)));
                                break;
                            }
                        }
                        case BAN:
                        {
                            final Ban ban = Ban.fromPlayer(player, "KoolSMPCore Text Filter", filter.getReason(), TimeOffset.getOffset("1d"));
                            plugin.getBanManager().addBan(ban);
                            plugin.getRecordKeeper().recordPunishment(Punishment.fromBan(ban));
                            player.kick(ban.getKickMessage(), PlayerKickEvent.Cause.BANNED);
                            Bukkit.getOnlinePlayers().stream().filter(suspect -> FUtil.getIp(player).equalsIgnoreCase(FUtil.getIp(player))).forEach(suspect ->
                                    player.kick(ban.getKickMessage()));
                            notifyAdmins(mmDeserialize("<red>Player has triggered the book filter | Content: '<message>'",
                                    Placeholder.unparsed("message", text)));
                            break;
                        }
                        case LOG:
                        {
                            FLog.warning(filter.getReason(), player.getName(), text);
                            notifyAdmins(mmDeserialize("<red>Player has triggered the book filter | Content: '<message>'",
                                    Placeholder.unparsed("message", text)));
                            break;
                        }
                        case MUTE:
                        {
                            MuteManager muteManager = plugin.getMuteManager();

                            if (!muteManager.isMuted(player))
                            {
                                muteManager.mute(player);
                                FUtil.staffAction(Bukkit.getConsoleSender(), "Muted <player>",
                                        Placeholder.unparsed("player", player.getName()));
                                plugin.getRecordKeeper().recordPunishment(Punishment.builder()
                                        .uuid(player.getUniqueId())
                                        .name(player.getName())
                                        .ip(player.isOnline() ? FUtil.getIp(player) : null)
                                        .by(Bukkit.getConsoleSender().getName())
                                        .type("MUTE")
                                        .build());
                            }
                            notifyAdmins(mmDeserialize("<red>Player has triggered the book filter | Content: '<message>'",
                                    Placeholder.unparsed("message", text)));
                        }
                        default:
                        {
                            // Do nothing
                        }
                    }
                }, filter.getDelay());
            });
        }
    }

    @EventHandler
    public void onAnvilPrepare(PrepareAnvilEvent event)
    {
        if (!(event.getView().getPlayer() instanceof Player player)) return;
        if (event.getResult() == null || !event.getResult().hasItemMeta()) return;
        if (!event.getResult().getItemMeta().hasDisplayName()) return;

        final String name = FUtil.plainText(event.getResult().getItemMeta().displayName());
        getFilterEntryIfApplicable(player, name).ifPresent(filter ->
        {
            if (filter.isCancel())
                event.setResult(null);

            Bukkit.getScheduler().runTaskLater(plugin, () ->
            {
                switch (filter.getAction())
                {
                    case DOOM:
                    {
                        // Only doom them if they are online, otherwise we just fall back to just banning them
                        if (player.isOnline())
                        {
                            DoomCommand.eviscerate(player, Bukkit.getConsoleSender(), filter.getReason());
                            notifyAdmins(mmDeserialize("<red>Player has triggered the anvil filter | Content: '<message>'",
                                    Placeholder.unparsed("message", name)));
                            break;
                        }
                    }
                    case SMITE:
                    {
                        if (player.isOnline())
                        {
                            SmiteCommand.zap(player, Bukkit.getConsoleSender(), filter.getReason());
                            notifyAdmins(mmDeserialize("<red>Player has triggered the anvil filter | Content: '<message>'",
                                    Placeholder.unparsed("message", name)));
                            break;
                        }
                    }
                    case BAN:
                    {
                        final Ban ban = Ban.fromPlayer(player, "KoolSMPCore Text Filter", filter.getReason(), TimeOffset.getOffset("1d"));
                        plugin.getBanManager().addBan(ban);
                        plugin.getRecordKeeper().recordPunishment(Punishment.fromBan(ban));
                        player.kick(ban.getKickMessage(), PlayerKickEvent.Cause.BANNED);
                        Bukkit.getOnlinePlayers().stream().filter(suspect -> FUtil.getIp(player).equalsIgnoreCase(FUtil.getIp(player))).forEach(suspect ->
                                player.kick(ban.getKickMessage()));
                        notifyAdmins(mmDeserialize("<red>Player has triggered the anvil filter | Content: '<message>'",
                                Placeholder.unparsed("message", name)));
                        break;
                    }
                    case LOG:
                    {
                        FLog.warning(filter.getReason(), player.getName(), name);
                        notifyAdmins(mmDeserialize("<red>Player has triggered the anvil filter | Content: '<message>'",
                                Placeholder.unparsed("message", name)));
                        break;
                    }
                    case MUTE:
                    {
                        MuteManager muteManager = plugin.getMuteManager();

                        if (!muteManager.isMuted(player))
                        {
                            muteManager.mute(player);
                            FUtil.staffAction(Bukkit.getConsoleSender(), "Muted <player>",
                                    Placeholder.unparsed("player", player.getName()));
                            plugin.getRecordKeeper().recordPunishment(Punishment.builder()
                                    .uuid(player.getUniqueId())
                                    .name(player.getName())
                                    .ip(player.isOnline() ? FUtil.getIp(player) : null)
                                    .by(Bukkit.getConsoleSender().getName())
                                    .type("MUTE")
                                    .build());
                        }
                        notifyAdmins(mmDeserialize("<red>Player has triggered the anvil filter | Content: '<message>'",
                                Placeholder.unparsed("message", name)));
                    }
                    default:
                    {
                        // Do nothing
                    }
                }
            }, filter.getDelay());
        });
    }

    @EventHandler
    public void onPlayerCMD(PlayerCommandPreprocessEvent event)
    {
        // Let's also check commands while we're at it :)
        if (ConfigEntry.CHAT_FILTER_ENABLED.getBoolean())
        {
            final Player player = event.getPlayer();
            final String cmd = event.getMessage();

            String content = cmd.startsWith("/") ? cmd.substring(1) : cmd;

            getFilterEntryIfApplicable(player, content).ifPresent(filter ->
            {
                if (filter.isCancel())
                    event.setCancelled(true);

                Bukkit.getScheduler().runTaskLater(plugin, () ->
                {
                    switch (filter.getAction())
                    {
                        case DOOM:
                        {
                            // Only doom them if they are online, otherwise we just fall back to just banning them
                            if (player.isOnline())
                            {
                                DoomCommand.eviscerate(player, Bukkit.getConsoleSender(), filter.getReason());
                                notifyAdmins(mmDeserialize("<red>Player has triggered the command filter | Content: '<message>'",
                                        Placeholder.unparsed("message", content)));
                                break;
                            }
                        }
                        case SMITE:
                        {
                            if (player.isOnline())
                            {
                                SmiteCommand.zap(player, Bukkit.getConsoleSender(), filter.getReason());
                                notifyAdmins(mmDeserialize("<red>Player has triggered the command filter | Content: '<message>'",
                                        Placeholder.unparsed("message", content)));
                                break;
                            }
                        }
                        case BAN:
                        {
                            final Ban ban = Ban.fromPlayer(player, "KoolSMPCore Text Filter", filter.getReason(), TimeOffset.getOffset("1d"));
                            plugin.getBanManager().addBan(ban);
                            plugin.getRecordKeeper().recordPunishment(Punishment.fromBan(ban));
                            player.kick(ban.getKickMessage(), PlayerKickEvent.Cause.BANNED);
                            Bukkit.getOnlinePlayers().stream().filter(suspect -> FUtil.getIp(player).equalsIgnoreCase(FUtil.getIp(player))).forEach(suspect ->
                                    player.kick(ban.getKickMessage()));
                            notifyAdmins(mmDeserialize("<red>Player has triggered the command filter | Content: '<message>'",
                                    Placeholder.unparsed("message", content)));
                            break;
                        }
                        case LOG:
                        {
                            FLog.warning(filter.getReason(), player.getName(), content);
                            notifyAdmins(mmDeserialize("<red>Player has triggered the command filter | Content: '<message>'",
                                    Placeholder.unparsed("message", content)));
                            break;
                        }
                        case MUTE:
                        {
                            MuteManager muteManager = plugin.getMuteManager();

                            if (!muteManager.isMuted(player))
                            {
                                muteManager.mute(player);
                                FUtil.staffAction(Bukkit.getConsoleSender(), "Muted <player>",
                                        Placeholder.unparsed("player", player.getName()));
                                plugin.getRecordKeeper().recordPunishment(Punishment.builder()
                                        .uuid(player.getUniqueId())
                                        .name(player.getName())
                                        .ip(player.isOnline() ? FUtil.getIp(player) : null)
                                        .by(Bukkit.getConsoleSender().getName())
                                        .type("MUTE")
                                        .build());
                            }
                            notifyAdmins(mmDeserialize("<red>Player has triggered the command filter | Content: '<message>'",
                                    Placeholder.unparsed("message", content)));
                        }
                        default:
                        {
                            // Do nothing
                        }
                    }
                }, filter.getDelay());
            });
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event)
    {
        if (ConfigEntry.CHAT_FILTER_ENABLED.getBoolean())
        {
            final Player player = event.getPlayer();
            final String message = event.signedMessage().message();

            getFilterEntryIfApplicable(player, message).ifPresent(filter ->
            {
                if (filter.isCancel())
                    event.setCancelled(true);

                Bukkit.getScheduler().runTaskLater(plugin, () ->
                {
                    switch (filter.getAction())
                    {
                        case DOOM:
                        {
                            // Only doom them if they are online, otherwise we just fall back to just banning them
                            if (player.isOnline())
                            {
                                DoomCommand.eviscerate(player, Bukkit.getConsoleSender(), filter.getReason());
                                notifyAdmins(mmDeserialize("<red>Player has triggered the chat filter | Content: '<message>'",
                                        Placeholder.unparsed("message", message)));
                                break;
                            }
                        }
                        case SMITE:
                        {
                            if (player.isOnline())
                            {
                                SmiteCommand.zap(player, Bukkit.getConsoleSender(), filter.getReason());
                                notifyAdmins(mmDeserialize("<red>Player has triggered the chat filter | Content: '<message>'",
                                        Placeholder.unparsed("message", message)));
                                break;
                            }
                        }
                        case BAN:
                        {
                            final Ban ban = Ban.fromPlayer(player, "KoolSMPCore Text Filter", filter.getReason(), TimeOffset.getOffset("1d"));
                            plugin.getBanManager().addBan(ban);
                            plugin.getRecordKeeper().recordPunishment(Punishment.fromBan(ban));
                            player.kick(ban.getKickMessage(), PlayerKickEvent.Cause.BANNED);
                            Bukkit.getOnlinePlayers().stream().filter(suspect -> FUtil.getIp(player).equalsIgnoreCase(FUtil.getIp(player))).forEach(suspect ->
                                    player.kick(ban.getKickMessage()));
                            notifyAdmins(mmDeserialize("<red>Player has triggered the chat filter | Content: '<message>'",
                                    Placeholder.unparsed("message", message)));
                            break;
                        }
                        case LOG:
                        {
                            FLog.warning(filter.getReason(), player.getName(), message);
                            notifyAdmins(mmDeserialize("<red>Player has triggered the chat filter | Content: '<message>'",
                                    Placeholder.unparsed("message", message)));
                            break;
                        }
                        case MUTE:
                        {
                            MuteManager muteManager = plugin.getMuteManager();

                            if (!muteManager.isMuted(player)) {
                                muteManager.mute(player);
                                FUtil.staffAction(Bukkit.getConsoleSender(), "Muted <player>",
                                        Placeholder.unparsed("player", player.getName()));
                                plugin.getRecordKeeper().recordPunishment(Punishment.builder()
                                        .uuid(player.getUniqueId())
                                        .name(player.getName())
                                        .ip(player.isOnline() ? FUtil.getIp(player) : null)
                                        .by(Bukkit.getConsoleSender().getName())
                                        .type("MUTE")
                                        .build());
                            }
                            notifyAdmins(mmDeserialize("<red>Player has triggered the chat filter | Content: '<message>'",
                                    Placeholder.unparsed("message", message)));
                        }
                        default:
                        {
                            // Do nothing
                        }
                    }
                }, filter.getDelay());
            });
        }
    }

    @Getter
    public static class FilterEntry
    {
        private final String name;
        private final String bypassPermission;
        private final FilterAction action;
        private final boolean cancel;
        private final String reason;
        private final int delay;
        private final List<Pattern> filters;

        private FilterEntry(String name, String bypassPermission, FilterAction action, boolean cancel, String reason, int delay, List<Pattern> filters)
        {
            this.name = name;
            this.bypassPermission = bypassPermission;
            this.action = action;
            this.cancel = cancel;
            this.reason = reason;
            this.delay = delay;
            this.filters = filters;
        }

        public static FilterEntry fromConfigurationSection(String name, ConfigurationSection section)
        {
            final String bypassPermission = section.getString("bypassPermission", null);
            final FilterAction action = FilterAction.getByName(section.getString("action", null));
            final boolean cancel = section.getBoolean("cancel", true);
            final String reason = section.getString("reason", StringUtils.capitalize(name.replaceAll("-_", " ")));
            final List<Pattern> filters = section.getStringList("filters").stream().map(entry ->
            {
                try
                {
                    return Pattern.compile(entry);
                }
                catch (PatternSyntaxException ex)
                {
                    FLog.warning("Skipping invalid pattern: {}", entry);
                    return null;
                }
            }).filter(Objects::nonNull).toList();
            final int delay = section.getInt("delay", 0);

            return new FilterEntry(name, bypassPermission, action, cancel, reason, delay, filters);
        }

        public boolean isApplicable(OfflinePlayer player, String message)
        {
            return (bypassPermission == null || (player instanceof Player online && !online.hasPermission(bypassPermission))) && filters.stream()
                    .anyMatch(filter -> filter.matcher(message).find());
        }

        public enum FilterAction
        {
            BAN,
            DOOM,
            LOG,
            MUTE,
            NOTHING,
            SMITE;

            public static FilterAction getByName(String name)
            {
                if (name == null)
                {
                    return NOTHING;
                }

                return Arrays.stream(values()).filter(action -> action.name().equalsIgnoreCase(name))
                        .findAny().orElse(NOTHING);
            }
        }
    }
}
