package eu.koolfreedom.bridge.discord;

import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.bridge.GroupManagement;
import eu.koolfreedom.config.ConfigEntry;
import eu.koolfreedom.bridge.DiscordIntegration;
import eu.koolfreedom.event.*;
import eu.koolfreedom.reporting.Report;
import eu.koolfreedom.util.FUtil;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePreProcessEvent;
import github.scarsz.discordsrv.api.events.DiscordReadyEvent;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.*;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.ButtonClickEvent;
import github.scarsz.discordsrv.dependencies.jda.api.hooks.ListenerAdapter;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.scarsz.discordsrv.util.*;
import net.kyori.adventure.key.Namespaced;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
public class DiscordSRVIntegration extends ListenerAdapter implements DiscordIntegration<DiscordSRV>
{
    private final DiscordSRV plugin;
    private final Namespaced key;

    private final GsonComponentSerializer nativeAdventureSerializer = GsonComponentSerializer.gson();
    private final github.scarsz.discordsrv.dependencies.kyori.adventure.text.serializer.gson.GsonComponentSerializer srvAdventureSerializer = github.scarsz.discordsrv.dependencies.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson();

    public DiscordSRVIntegration()
    {
        plugin = DiscordSRV.getPlugin();
        key = NamespacedKey.fromString("channel", plugin);

        // So the idea behind this was that instead of doing Discord integration ourselves (causing obscene amounts of
        //  bloat and adding a potential risk of people finding ways to bypass our homebrew filters due to Discord being
        //  Discord), we would just hook into other plugins that handled Discord integration for us and just use their
        //  APIs and embedded Discord libraries for what we need.
        //
        // Slight problem. There's a good chance that most of what we've done here probably isn't officially supported
        //  because in a lot of places we're straight up ignoring DiscordSRV's own ways of doing things, calling
        //  internal functions intended for, well, internal use, and bypassing the DiscordSRV API where we see fit
        //  because of quirks with that plugin.
        //
        // Known quirks:
        //  - We have to register ourselves as three different types of event listeners: Bukkit, DiscordSRV, and JDA.
        //    Our codebase uses the native Bukkit API to communicate important information to bridges and other plugins
        //    while also greatly reducing the amount of direct calls for things like server broadcasts, the admin chat,
        //    and reports. DiscordSRV, despite being a Bukkit plugin, uses their own weird ass event system. The only
        //    reasons I could really see them doing this for is to circumvent some quirks that Bukkit's event system
        //    has. Maybe they did it so you don't have to implement Listener to listen for DiscordSRV-specific events?
        //    Who knows, but it's a hassle on our end since we now have to hook into it to find out when JDA is
        //    set up and available for us to mess with and also handle messages. JDA itself has an excuse since it is
        //    primarily intended as a library for Java-based Discord bots, not just Minecraft servers. We have to extend
        //    its ListenerAdapter to listen for events not covered by DiscordSRV's API, like action bar interactions,
        //    for use in our report system.
        //
        //  - We don't strictly respect the DiscordSRV configuration and how it handles messages. The admin chat system
        //    uses something completely specific to us, which includes the format and how it works. If memory serves,
        //    DiscordSRV *does* have support for channel-specific formatting. However, it only supports a limited
        //    selection of placeholders (thus being very inflexible), would be really annoying for server owners to have
        //    to maintain two different formats in two different configurations for two different plugins for a single
        //    system, and it would look stupid having an admin chat format that would look different if it came from
        //    Discord. So, we just try to replicate DiscordSRV's behavior but add our own spice to the mix where
        //    possible, and while this *does* work, we would have to effectively replicate a lot of code like how
        //    messages are process and have to add even more dependencies to a project like PlaceholderAPI. Oh well, at
        //    least we have a universal format for the admin chat from Discord to Minecraft while also maintaining some
        //    behavioral consistency with regular DiscordSRV.
        //
        //  - We selectively choose between using the official API and some functions clearly intended for internal use.
        //    This one, we have no choice. DiscordSRV's API straight up lacks methods to get channels directly, which is
        //    what we need to do to get channels like the admin chat and player reports. Furthermore, the internal
        //    method for easily getting a channel (getOptionalTextChannel()) will return the main text channel if the
        //    channel we are looking for isn't defined. Since we are working with information intended to be seen by
        //    staff only, this is completely unacceptable. So, we work around that by just doing some checks beforehand
        //    to see if the channel is registered. If it is there, then we proceed to get it since then we know we won't
        //    leak anything. Plus, text components are a PITA. Speaking of which...
        //
        //  - We do a lot of back and forth conversion for text components. DiscordSRV includes its own relocated
        //    version of Adventure, presumably for compatibility with Spigot servers which don't include Adventure, but
        //    the downside of including a relocated version of Adventure is that while they are theoretically compatible
        //    with Paper's versions since they are basically the fucking same version of the same objects in memory,
        //    they simply aren't. This causes a bunch of headaches that require us to use two different Adventure
        //    instances' GSON serializers just to convert two things which would otherwise be completely fine in a
        //    perfect world just to use certain internal functions.
        //
        // The one good thing is that with some of these hacks in place, I now know a bit more about how much work I
        //  would need to do if management over at KoolSMP decided to just shitcan DiscordSRV altogether and use an
        //  in-house system built with JDA.
    }


    @Override
    public DiscordSRV getDiscord()
    {
        return plugin;
    }

    @Override
    public DiscordIntegration<DiscordSRV> register()
    {
        Bukkit.getPluginManager().registerEvents(this, KoolSMPCore.getInstance());
        DiscordSRV.api.subscribe(this);
        return this;
    }

    @Override
    public boolean channelExists(String name)
    {
        return plugin.getChannels().containsKey(name);
    }

    // -- DISCORDSRV EVENTS -- //

    @Subscribe
    public void onDiscordReady(DiscordReadyEvent event)
    {
        plugin.getJda().addEventListener(this);
    }

    @Subscribe
    public void onAdminChatMessageFromDiscord(DiscordGuildMessagePreProcessEvent event)
    {
        if (!channelExists("adminchat") || channelDoesNotMatch("adminchat", event.getChannel().getId())
                || event.getAuthor() == plugin.getJda().getSelfUser())
        {
            return;
        }

        final GroupManagement.Group fallback = KoolSMPCore.getInstance().getGroupManager().getGroupByNameOr("discord", discordGroup);

        final Member member = event.getMember();
        final Component message = convert(MessageUtil.reserializeToMinecraft(event.getMessage().getContentRaw()));
        final GroupManagement.Group userGroup = switch (ConfigEntry.DISCORD_GROUP_MODE_SWITCH.getInteger())
        {
            case 1 -> KoolSMPCore.getInstance().getGroupManager().getGroupByNameOr(member.getRoles().getFirst().getName(),
                    !member.getRoles().isEmpty() ? getGroupFromRole(member.getRoles().getFirst()) : fallback);
            case 2 -> !member.getRoles().isEmpty() ? getGroupFromRole(member.getRoles().getFirst()) : fallback;
            default -> fallback;
        };

        // Message reply
        Component reply = Component.empty();
        if (event.getMessage().getReferencedMessage() != null)
        {
            final Message replyMessage = event.getMessage().getReferencedMessage();
            final Component replyComponent = convert(MessageUtil.reserializeToMinecraft(replyMessage.getContentRaw()));
            final Member replyMember = replyMessage.getMember();

            reply = FUtil.miniMessage(ConfigEntry.DISCORD_REPLYING_TO_FORMAT.getString(),
                    Placeholder.parsed("message_id", replyMessage.getId()),
                    Placeholder.parsed("user_id", replyMessage.getAuthor().getId()),
                    Placeholder.parsed("username", replyMessage.getAuthor().getName()),
                    Placeholder.parsed("name", replyMessage.getAuthor().getEffectiveName()),
                    Placeholder.unparsed("nickname", replyMember != null ? replyMember.getEffectiveName() :
                            replyMessage.getAuthor().getEffectiveName()),
                    replyMember != null ?
                            Placeholder.styling("role_color", (builder) -> builder.color(TextColor.color(replyMember.getColorRaw()))) :
                            Placeholder.component("role_color", Component.empty()),
                    Formatter.booleanChoice("if_has_attachments", !replyMessage.getAttachments().isEmpty()),
                    Placeholder.component("attachments", Component.join(JoinConfiguration.newlines(),
                            replyMessage.getAttachments().stream().map(attachment -> Component.text(attachment.getUrl())).toList())),
                    Placeholder.component("message", replyComponent),
                    Formatter.date("date_created", replyMessage.getTimeCreated()),
                    Formatter.booleanChoice("if_edited", replyMessage.getTimeEdited() != null),
                    replyMessage.getTimeEdited() != null ?
                            Formatter.date("date_edited", replyMessage.getTimeEdited()) :
                            Placeholder.component("date_edited", Component.empty()),
                    Placeholder.component("roles", member.getRoles().isEmpty() ?
                            Component.text("(none)").color(NamedTextColor.GRAY) :
                            Component.text(" - ").color(NamedTextColor.GRAY).append(Component.join(JoinConfiguration.separator(Component.newline()
                                            .append(Component.text(" - ").color(NamedTextColor.GRAY))),
                                    member.getRoles().stream().map(role -> Component.text(role.getName()).color(TextColor.color(role.getColorRaw()))).toList()))));
        }

        final Component displayName = FUtil.miniMessage(ConfigEntry.DISCORD_USER_FORMAT.getString(),
                Placeholder.parsed("id", member.getId()),
                Placeholder.parsed("username", member.getUser().getName()),
                Placeholder.parsed("name", member.getUser().getEffectiveName()),
                Placeholder.unparsed("nickname", member.getEffectiveName()),
                Placeholder.styling("role_color", (builder) -> builder.color(TextColor.color(member.getColorRaw()))),
                Placeholder.component("roles", member.getRoles().isEmpty() ?
                        Component.text("(none)").color(NamedTextColor.GRAY) :
                        Component.text(" - ").color(NamedTextColor.GRAY).append(Component.join(JoinConfiguration.separator(Component.newline()
                                        .append(Component.text(" - ").color(NamedTextColor.GRAY))),
                member.getRoles().stream().map(role -> Component.text(role.getName()).color(TextColor.color(role.getColorRaw()))).toList()))),
                Placeholder.component("reply", reply));

        // Call separate events for attachments to emulate DiscordSRV behavior
        event.getMessage().getAttachments().forEach(attachment ->
                FUtil.asyncAdminChat(displayName, member.getUser().getName(), userGroup,
						Component.text(attachment.getUrl()).clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL,
								attachment.getUrl())), key));

        FUtil.asyncAdminChat(displayName, member.getUser().getName(), userGroup, message, key);
    }

    // -- JDA EVENTS -- //

    @Override
    public void onButtonClick(ButtonClickEvent event)
    {
        if (channelDoesNotMatch("reports", event.getChannel().getId())
                || event.getMember() == null
                || event.getButton() == null)
        {
            return;
        }

        final Message message = event.getMessage();
        final Button button = event.getButton();
        final Member member = event.getMember();
        final Component userDisplay = Component.text(member.getAsMention());
        final Optional<Report> optionalReport = KoolSMPCore.getInstance().getReportManager().getReports(true).stream().filter(report ->
                report.getAdditionalData().getString("discordMessageId", "-1").equalsIgnoreCase(message.getId())).findAny();

        if (message.getAuthor() == plugin.getJda().getSelfUser() && button.getId() != null)
        {
            switch (button.getId().toLowerCase())
            {
                case "handled" ->
                {
                    if (lacksPermission(member, "kfc.command.reports.handle"))
                    {
                        event.reply("You don't have permission to do that.").setEphemeral(true).queue();
                        return;
                    }

                    optionalReport.ifPresentOrElse(report ->
                    {
                        if (report.isResolved())
                        {
                            event.reply("This report has already been resolved.").setEphemeral(true).queue();
                            return;
                        }

                        report.updateAsync(userDisplay, member.getUser().getName(), member.getUser().getId(), Report.ReportStatus.RESOLVED, "Handled");
                        event.reply("Report " + report.getId() + " has been marked as handled.").setEphemeral(true).queue();
                    }, () -> message.editMessage("Unlinked report resolved by " + member.getAsMention())
                            .setEmbeds(message.getEmbeds()).queue());
                }
                case "invalid" ->
                {
                    if (lacksPermission(member, "kfc.command.reports.close"))
                    {
                        event.reply("You don't have permission to do that.").setEphemeral(true).queue();
                        return;
                    }

                    optionalReport.ifPresentOrElse(report ->
                    {
                        if (report.isResolved())
                        {
                            event.reply("This report has already been resolved.").setEphemeral(true).queue();
                            return;
                        }

                        report.updateAsync(userDisplay, member.getUser().getName(), member.getUser().getId(), Report.ReportStatus.CLOSED, "Invalid");
                        event.reply("Report " + report.getId() + " has been closed.").setEphemeral(true).queue();
                    }, () ->
                    {
                        message.delete().queue();
                        event.reply("This report didn't even have a message ID associated to it somehow, so we deleted it.").setEphemeral(true).queue();
                    });
                }
                case "purge" ->
                {
                    if (lacksPermission(member, "kfc.command.reports.purge"))
                    {
                        event.reply("You don't have permission to do that.").setEphemeral(true).queue();
                        return;
                    }

                    optionalReport.ifPresentOrElse(report ->
                    {
                        KoolSMPCore.getInstance().getReportManager().deleteReportsByUuidAsync(userDisplay, member.getUser().getName(), member.getUser().getId(), report.getReporter());
                        event.reply("Reports are now being deleted as we speak.").setEphemeral(true).queue();
                    }, () ->
                    {
                        message.delete().queue();
                        event.reply("This report didn't even have a message ID associated to it somehow, so we couldn't delete every report this user filed.").setEphemeral(true).queue();
                    });
                }
                case "reopen" ->
                {
                    if (lacksPermission(member, "kfc.command.reports.reopen"))
                    {
                        event.reply("You don't have permission to do that.").setEphemeral(true).queue();
                        return;
                    }

                    optionalReport.ifPresentOrElse(report ->
                    {
                        if (!report.isResolved())
                        {
                            event.reply("This report is already open.").setEphemeral(true).queue();
                            return;
                        }

                        report.updateAsync(userDisplay, member.getUser().getName(), member.getUser().getId(), Report.ReportStatus.REOPENED, "Reopened for further investigation");
                        event.reply("Report " + report.getId() + " has been re-opened.").setEphemeral(true).queue();
                    }, () -> event.reply("This report doesn't have a message ID associated to it, so we can't re-open it.").setEphemeral(true).queue());
                }
            }
        }
    }

    // -- BUKKIT EVENTS -- //

    @EventHandler
    private void onReport(PlayerReportEvent event)
    {
        if (!channelExists("reports"))
        {
            return;
        }

        final Report report = event.getReport();

        plugin.getOptionalTextChannel("reports")
                .sendMessageEmbeds(createEmbedFromReport(report))
                .setActionRow(createButtonsFromReport(report))
                .queue(message ->
                {
                    report.getAdditionalData().set("discordMessageId", message.getId());
                    CompletableFuture.runAsync(() -> KoolSMPCore.getInstance().getReportManager().save());
                });
    }

    @EventHandler
    private void onReportUpdated(PlayerReportUpdateEvent event)
    {
        if (!channelExists("reports"))
        {
            return;
        }

        final Report report = event.getReport();
        final TextChannel channel = plugin.getOptionalTextChannel("reports");
        AtomicBoolean createMessage = new AtomicBoolean(false);

        if (!report.getAdditionalData().isString("discordMessageId"))
        {
            createMessage.set(true);
        }

        channel.retrieveMessageById(Objects.requireNonNull(report.getAdditionalData().getString("discordMessageId")))
                .queue(message ->
                                message.editMessage("Report updated by " + FUtil.plainText(event.getStaffDisplayName()))
                                .setEmbeds(createEmbedFromReport(report))
                                .setActionRow(createButtonsFromReport(report)).queue(),
                        error -> createMessage.set(true));

        if (createMessage.get())
        {
            channel.sendMessage("Report updated by " + event.getStaffName())
                    .setEmbeds(createEmbedFromReport(report))
                    .setActionRow(createButtonsFromReport(report))
                    .queue(success ->
                    {
                        report.getAdditionalData().set("discordMessageId", success.getId());
                        CompletableFuture.runAsync(() -> KoolSMPCore.getInstance().getReportManager().save());
                    });
        }
    }

    @EventHandler
    private void onReportDeleted(PlayerReportDeleteEvent event)
    {
        if (!channelExists("reports"))
        {
            return;
        }

        final TextChannel channel = plugin.getOptionalTextChannel("reports");
        channel.purgeMessagesById(event.getReports().stream().filter(report -> report.getAdditionalData().isString("discordMessageId"))
                .map(report -> report.getAdditionalData().getString("discordMessageId")).toList());
    }

    @EventHandler
    private void onBroadcast(PublicBroadcastEvent event)
    {
        final TextChannel channel = plugin.getOptionalTextChannel("broadcasts");
        Component message = event.getMessage();

        if (message.decorations().entrySet().stream().allMatch(entry -> entry.getValue().equals(TextDecoration.State.NOT_SET)
                || entry.getValue().equals(TextDecoration.State.FALSE)))
        {
            message = message.decorate(net.kyori.adventure.text.format.TextDecoration.BOLD);
        }

        channel.sendMessage(MessageUtil.reserializeToDiscord(convert(message))).queue();
    }

    @EventHandler
    public void onAdminChat(AdminChatEvent event)
    {
        // Do not call duplicate events and don't do anything if we do not have a designated adminchat channel to avoid
        //  potential confidentiality leaks
        if (event.getSource().equals(key) || !channelExists("adminchat"))
        {
            return;
        }

        final TextChannel channel = plugin.getOptionalTextChannel("adminchat");
        final CommandSender sender = event.getCommandSender();
        final String senderName = event.getSenderName() != null ? event.getSenderName() : Objects.requireNonNull(sender).getName();

        boolean usingWebhooks = DiscordSRV.config().getBoolean("Experiment_WebhookChatMessageDelivery");

        // Did it come from the server itself
        if (sender instanceof Player player)
        {
            plugin.processChatMessage(player, convert(event.getMessage()), "adminchat", false, event);
            return;
        }

        String displayName;
        String webhookAvatar = plugin.getJda().getSelfUser().getAvatarUrl();
        final String message = MessageUtil.reserializeToDiscord(convert(event.getMessage()));

        // The command came from someone running the command from within console
        if (sender instanceof PaperForwardingCommandSender srv)
        {
            displayName = plugin.getJda().getSelfUser().getEffectiveName();
        }
        // The command came from a
        else if (sender instanceof ConsoleCommandSender || sender instanceof RemoteConsoleCommandSender)
        {
            displayName = sender.getName();
        }
        else if (sender instanceof Mob mob)
        {
            displayName = FUtil.plainText(mob.customName());
        }
        else
        {
            displayName = FUtil.plainText(event.getSenderDisplay());
        }

        // TODO: Implement more... "native" support instead of this hackjob, lmao

        if (DiscordSRV.config().getBoolean("Experiment_WebhookChatMessageDelivery"))
        {
            WebhookUtil.deliverMessage(channel, displayName, webhookAvatar, message, List.of());
        }
        else
        {
            DiscordUtil.sendMessage(channel, LangUtil.Message.CHAT_TO_DISCORD_NO_PRIMARY_GROUP.toString()
                    .replace("%username%", DiscordUtil.escapeMarkdown(senderName))
                    .replace("%usernamenoescapes%", senderName)
                    .replace("%displayname%", DiscordUtil.escapeMarkdown(displayName))
                    .replace("%displaynamenoescapes%", displayName)
                    .replace("%message%", message)
            );
        }
    }

    // -- UTILITY/CONVERSION METHODS -- //

    private boolean channelDoesNotMatch(String name, String channelId)
    {
        return !channelId.equals(plugin.getChannels().get(name));
    }

    private List<Button> createButtonsFromReport(Report report)
    {
        return report.isResolved() ?
                List.of(Button.primary("reopen", "Re-open")) :
                //--
                List.of(Button.primary("handled", "Resolve"),
                        Button.primary("invalid", "Invalid"),
                        Button.danger("purge", "Purge"));
    }

    private GroupManagement.Group getGroupFromRole(Role role)
    {
        final String roleId = role.getId();

        if (!roleMap.containsKey(roleId) || roleMap.get(roleId).getColor().value() != role.getColorRaw() ||
                !roleMap.get(roleId).getName().equalsIgnoreCase(role.getName()))
        {
            roleMap.remove(roleId);

            GroupManagement.Group roleGroup = GroupManagement.Group.createGroup(role.getId(), role.getName(),
                    Component.text(role.getName()).color(TextColor.color(role.getColorRaw())),
                    TextColor.color(role.getColorRaw()));

            roleMap.put(roleId, roleGroup);
        }

        return roleMap.get(roleId);
    }

    private github.scarsz.discordsrv.dependencies.kyori.adventure.text.Component convert(Component component)
    {
        return srvAdventureSerializer.deserialize(nativeAdventureSerializer.serialize(component));
    }

    private Component convert(github.scarsz.discordsrv.dependencies.kyori.adventure.text.Component component)
    {
        return nativeAdventureSerializer.deserialize(srvAdventureSerializer.serialize(component));
    }

    private MessageEmbed createEmbedFromReport(Report report)
    {
        final OfflinePlayer reporter = Bukkit.getOfflinePlayer(report.getReporter());
        final OfflinePlayer reported = Bukkit.getOfflinePlayer(report.getReported());
        final String reason = report.getReason();

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle((report.getStatus() != Report.ReportStatus.UNRESOLVED ? "**" + report.getStatus().name() + "**: " : "") + "Report for " + reported.getName() + (!reported.isOnline() ? " (offline)" : "") )
                .setColor(report.getStatus().getAwtColor())
                .setDescription(reason)
                .setFooter("ID " + report.getId() + " • Reported by " + reporter.getName(), "https://minotar.net/helm/" + reporter.getName() + ".png")
                .setTimestamp(ZonedDateTime.of(LocalDateTime.ofEpochSecond(report.getTimestamp(), 0, ZoneId.systemDefault().getRules().getOffset(Instant.now())), ZoneId.systemDefault()).toOffsetDateTime());

        if (report.getLastNote() != null)
        {
            embed.addField("Staff Note", report.getLastNote(), true);
        }

        return embed.build();
    }

    private boolean lacksPermission(Member member, String permission)
    {
        final UUID uuid = getDiscord().getAccountLinkManager().getUuid(member.getId());

        if (uuid == null)
        {
            return true;
        }

        final OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        if (!player.isOnline() && !player.hasPlayedBefore())
        {
            return true;
        }

        return !KoolSMPCore.getInstance().getGroupManager().getVaultPermissions().playerHas(null, player, permission);
    }
}
