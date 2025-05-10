package eu.koolfreedom.discord;

import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.config.ConfigEntry;
import eu.koolfreedom.event.PlayerReportDeleteEvent;
import eu.koolfreedom.event.PlayerReportEvent;
import eu.koolfreedom.event.PlayerReportUpdateEvent;
import eu.koolfreedom.log.FLog;
import eu.koolfreedom.reporting.Report;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class Discord extends ListenerAdapter implements Listener
{
    private JDA jda;

    public Discord()
    {
        final String token = ConfigEntry.DISCORD_BOT_TOKEN.getString();

        // No token could be found, so we're going to assume the user doesn't want it
        if (token == null || token.isEmpty())
        {
            return;
        }

        try
        {
            jda = JDABuilder.createLight(token)
                    .setEnabledIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    .setDisabledIntents(GatewayIntent.GUILD_VOICE_STATES)
                    .build()
                    .awaitReady();

            FLog.info("Discord bot initialized");
        }
        catch (Exception ex)
        {
            FLog.error("Failed to initialize Discord bot", ex);
            return;
        }

        Bukkit.getPluginManager().registerEvents(this, KoolSMPCore.getInstance());
        jda.addEventListener(this);
    }

    public void shutdown()
    {
        if (jda != null)
        {
            jda.shutdownNow();
        }
    }

    public boolean isEnabled()
    {
        return jda != null;
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event)
    {
        if (event.getChannelId() == null
                || !event.getChannelId().equalsIgnoreCase(ConfigEntry.DISCORD_REPORTS_CHANNEL_ID.getString())
                || event.getGuild() == null || !event.getGuild().getId().equalsIgnoreCase(ConfigEntry.DISCORD_SERVER_ID.getString())
                || event.getMember() == null)
        {
            return;
        }

        final Message message = event.getMessage();
        final Button button = event.getButton();
        final Member member = event.getMember();
        final Component userDisplay = Component.text(member.getAsMention());
        final Optional<Report> optionalReport = KoolSMPCore.getInstance().reportManager.getReports(true).stream().filter(report ->
                report.getAdditionalData().getString("discordMessageId", "-1").equalsIgnoreCase(message.getId())).findAny();

        if (message.getAuthor() == jda.getSelfUser() && button.getId() != null)
        {
            switch (button.getId().toLowerCase())
            {
                case "handled" ->
                {
                    if (!isAuthorized(member, ConfigEntry.DISCORD_REPORTS_ROLES_THAT_CAN_RESOLVE.getStringList()))
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
                        event.reply("Report " + report.getId() + " has been re-handled.").setEphemeral(true).queue();
                    }, () -> message.editMessage(MessageEditData.fromCreateData(new MessageCreateBuilder()
                            .addContent("Unlinked report resolved by " + member.getAsMention())
                            .addEmbeds(message.getEmbeds())
                            .build())).queue());
                }
                case "invalid" ->
                {
                    if (!isAuthorized(member, ConfigEntry.DISCORD_REPORTS_ROLES_THAT_CAN_CLOSE.getStringList()))
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
                    if (!isAuthorized(member, ConfigEntry.DISCORD_REPORTS_ROLES_THAT_CAN_PURGE.getStringList()))
                    {
                        event.reply("You don't have permission to do that.").setEphemeral(true).queue();
                        return;
                    }

                    optionalReport.ifPresentOrElse(report ->
                    {
                        KoolSMPCore.getInstance().reportManager.deleteReportsByUuidAsync(userDisplay, member.getUser().getName(), member.getUser().getId(), report.getReporter());
                        event.reply("Reports are now being deleted as we speak.").setEphemeral(true).queue();
                    }, () ->
                    {
                        message.delete().queue();
                        event.reply("This report didn't even have a message ID associated to it somehow, so we couldn't delete every report this user filed.").setEphemeral(true).queue();
                    });
                }
                case "reopen" ->
                {
                    if (!isAuthorized(member, ConfigEntry.DISCORD_REPORTS_ROLES_THAT_CAN_REOPEN.getStringList()))
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

    @EventHandler
    private void onReport(PlayerReportEvent event)
    {
        final Report report = event.getReport();
        if (ConfigEntry.DISCORD_SERVER_ID.getString().isBlank() || ConfigEntry.DISCORD_REPORTS_CHANNEL_ID.getString().isBlank())
        {
            return;
        }

        Guild guild = jda.getGuildById(ConfigEntry.DISCORD_SERVER_ID.getString());

        if (guild != null)
        {
            TextChannel channel = guild.getTextChannelById(ConfigEntry.DISCORD_REPORTS_CHANNEL_ID.getString());

            if (channel != null)
            {
                try
                {
                    channel.sendMessageEmbeds(createEmbedFromReport(report))
                            .addActionRow(createButtonsFromReport(report))
                            .queue(message ->
                            {
                                report.getAdditionalData().set("discordMessageId", message.getId());
                                CompletableFuture.runAsync(() -> KoolSMPCore.getInstance().reportManager.save());
                            });
                }
                catch (Exception ex)
                {
                    FLog.error("Failed to send report data to Discord", ex);
                }
            }
        }
    }

    @EventHandler
    private void onReportUpdated(PlayerReportUpdateEvent event)
    {
        final Report report = event.getReport();
        if (ConfigEntry.DISCORD_SERVER_ID.getString().isBlank() || ConfigEntry.DISCORD_REPORTS_CHANNEL_ID.getString().isBlank())
        {
            return;
        }

        if (!report.getAdditionalData().isString("discordMessageId"))
        {
            return;
        }

        Guild guild = jda.getGuildById(ConfigEntry.DISCORD_SERVER_ID.getString());

        if (guild != null)
        {
            TextChannel channel = guild.getTextChannelById(ConfigEntry.DISCORD_REPORTS_CHANNEL_ID.getString());

            if (channel != null)
            {
                channel.retrieveMessageById(Objects.requireNonNull(report.getAdditionalData().getString("discordMessageId"))).queue(message ->
						message.editMessage(MessageEditData.fromCreateData(new MessageCreateBuilder()
								.addContent("Report updated by " + event.getStaffName())
								.addEmbeds(createEmbedFromReport(report))
								.addActionRow(createButtonsFromReport(report))
								.build())).queue(),
                        (ignored) ->
								channel.sendMessage("Report updated by " + event.getStaffName())
										.addEmbeds(createEmbedFromReport(report))
										.addActionRow(createButtonsFromReport(report))
										.queue(success ->
                                                {
                                                    report.getAdditionalData().set("discordMessageId", success.getId());
                                                    CompletableFuture.runAsync(() -> KoolSMPCore.getInstance().reportManager.save());
                                                }));
            }
        }
    }

    @EventHandler
    private void onReportDeleted(PlayerReportDeleteEvent event)
    {
        if (ConfigEntry.DISCORD_SERVER_ID.getString().isBlank() || ConfigEntry.DISCORD_REPORTS_CHANNEL_ID.getString().isBlank())
        {
            return;
        }

        Guild guild = jda.getGuildById(ConfigEntry.DISCORD_SERVER_ID.getString());

        if (guild != null)
        {
            TextChannel channel = guild.getTextChannelById(ConfigEntry.DISCORD_REPORTS_CHANNEL_ID.getString());

            if (channel != null)
            {
                channel.purgeMessagesById(event.getReports().stream().filter(report -> report.getAdditionalData().isString("discordMessageId"))
                        .map(report -> report.getAdditionalData().getString("discordMessageId")).toList());
            }
        }
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
                .setTimestamp(ZonedDateTime.now());

        if (report.getLastNote() != null)
        {
            embed.addField("Staff Note", report.getLastNote(), true);
        }

        return embed.build();
    }

    private boolean isAuthorized(Member member, List<String> roleIds)
    {
        return member.getRoles().stream().anyMatch(role -> roleIds.contains(role.getId()));
    }
}
