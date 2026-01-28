package eu.koolfreedom.bridge.discord;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.config.EssentialsConfiguration;
import com.earth2me.essentials.utils.FormatUtil;
import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.bridge.GroupManagement;
import eu.koolfreedom.config.ConfigEntry;
import eu.koolfreedom.bridge.DiscordIntegration;
import eu.koolfreedom.event.*;
import eu.koolfreedom.util.FLog;
import eu.koolfreedom.reporting.Report;
import eu.koolfreedom.util.FUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.essentialsx.api.v2.ChatType;
import net.essentialsx.api.v2.events.discord.DiscordMessageEvent;
import net.essentialsx.api.v2.services.discord.DiscordService;
import net.essentialsx.api.v2.services.discord.MessageType;
import net.essentialsx.api.v2.services.discordlink.DiscordLinkService;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.essentialsx.discord.DiscordSettings;
import net.essentialsx.discord.JDADiscordService;
import net.essentialsx.discord.util.DiscordUtil;
import net.essentialsx.discord.util.MessageUtil;
import net.kyori.adventure.key.Namespaced;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class EssentialsXDiscordIntegration implements DiscordIntegration<JDADiscordService>
{
	private final JDADiscordService service;
	private final DiscordLinkService linkService;
	private final Essentials essentials;
	private final Namespaced key;

	public EssentialsXDiscordIntegration()
	{
		service = (JDADiscordService) Bukkit.getServicesManager().load(DiscordService.class);
		linkService = Objects.requireNonNull(Bukkit.getServicesManager().load(DiscordLinkService.class));

		key = NamespacedKey.fromString("channel", Objects.requireNonNull(service).getPlugin());
		essentials = (Essentials) Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("Essentials"));

		new ReportsMessageType(this).register();
		new BroadcastMessageType(this).register();
		new AdminChatType(this).register();
	}

	@Override
	public JDADiscordService getDiscord()
	{
		return service;
	}

	@Override
	public boolean channelExists(String name)
	{
		return service.getChannel(name, false) != null;
	}

	@Getter
	@RequiredArgsConstructor
	public static class ReportsMessageType extends ListenerAdapter implements Listener
	{
		private final EssentialsXDiscordIntegration parent;
		private final MessageType type = new MessageType("reports");

		@Override
		public void onButtonInteraction(@NotNull ButtonInteractionEvent event)
		{
			if (parent.channelDoesNotMatch("reports", event.getChannel().getId()) || event.getMember() == null)
			{
				return;
			}

			final Message message = event.getMessage();
			final Button button = event.getButton();
			final Member member = event.getMember();
			final Component userDisplay = Component.text(member.getAsMention());
			final Optional<Report> optionalReport = KoolSMPCore.getInstance().getReportManager().getReports(true).stream().filter(report ->
					report.getAdditionalData().getString("discordMessageId", "-1").equalsIgnoreCase(message.getId())).findAny();

			if (message.getAuthor() == parent.getDiscord().getJda().getSelfUser() && button.getId() != null)
			{
				switch (button.getId().toLowerCase())
				{
					case "handled" ->
					{
						if (parent.lacksPermission(member, "kfc.command.reports.handle"))
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
						if (parent.lacksPermission(member, "kfc.command.reports.close"))
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
						if (parent.lacksPermission(member, "kfc.command.reports.purge"))
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
						if (parent.lacksPermission(member, "kfc.command.reports.reopen"))
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
			if (!getParent().channelExists("reports"))
			{
				return;
			}

			final Report report = event.getReport();

			getParent().getDiscord().getDefinedChannel("reports", false)
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
			if (!getParent().channelExists("reports"))
			{
				return;
			}

			final Report report = event.getReport();
			final TextChannel channel = getParent().getDiscord().getDefinedChannel("reports", false);
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
			if (!getParent().channelExists("reports"))
			{
				return;
			}

			final TextChannel channel = getParent().getDiscord().getDefinedChannel("reports", false);
			channel.purgeMessagesById(event.getReports().stream().filter(report -> report.getAdditionalData().isString("discordMessageId"))
					.map(report -> report.getAdditionalData().getString("discordMessageId")).toList());
		}

		public void register()
		{
			Bukkit.getPluginManager().registerEvents(this, KoolSMPCore.getInstance());
			getParent().getDiscord().getJda().addEventListener(this);
		}

		private MessageEmbed createEmbedFromReport(Report report)
		{
			final OfflinePlayer reporter = Bukkit.getOfflinePlayer(report.getReporter());
			final OfflinePlayer reported = Bukkit.getOfflinePlayer(report.getReported());
			final String reason = report.getReason();

			final List<MessageEmbed.Field> fields = new ArrayList<>();

			if (report.getLastNote() != null)
			{
				fields.add(new MessageEmbed.Field("Staff Note", report.getLastNote(), true));
			}

			return new MessageEmbed(null,
					(report.getStatus() != Report.ReportStatus.UNRESOLVED ? "**" + report.getStatus().name() + "**: " : "") + "Report for " + reported.getName() + (!reported.isOnline() ? " (offline)" : ""),
					reason,
					EmbedType.RICH,
					ZonedDateTime.of(LocalDateTime.ofEpochSecond(report.getTimestamp(), 0, ZoneId.systemDefault().getRules().getOffset(Instant.now())), ZoneId.systemDefault()).toOffsetDateTime(),
					report.getStatus().getAwtColor().getRGB(),
					null,
					null,
					null,
					null,
					new MessageEmbed.Footer("ID " + report.getId() + " " + " • Reported by " + reporter.getName(), "https://minotar.net/helm/" + reporter.getName() + ".png", null),
					null,
					fields);
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
	}

	@Getter
	@RequiredArgsConstructor
	public static class BroadcastMessageType implements Listener
	{
		private final EssentialsXDiscordIntegration parent;
		private final MessageType type = new MessageType("broadcast");

		@EventHandler
		public void onPublicBroadcast(PublicBroadcastEvent event)
		{
			final Component message = event.getMessage();
			parent.getDiscord().sendMessage(type, MessageUtil.sanitizeDiscordMarkdown(FUtil.plainText(message)), false);
		}

		public void register()
		{
			parent.getDiscord().registerMessageType(KoolSMPCore.getInstance(), type);
			Bukkit.getPluginManager().registerEvents(this, KoolSMPCore.getInstance());
		}
	}

	@Getter
	@RequiredArgsConstructor
	public static class AdminChatType extends ListenerAdapter implements Listener
	{
		private final EssentialsXDiscordIntegration parent;
		private final MessageType type = new MessageType("adminchat");

		@EventHandler
		public void onAdminChat(AdminChatEvent event)
		{
			if (event.getSource().equals(getParent().key) || !parent.channelExists("adminchat"))
			{
				return;
			}

			final CommandSender sender = event.getCommandSender();
			final DiscordSettings settings = parent.getDiscord().getSettings();
			final String message = FUtil.plainText(event.getMessage());

			final String avatarUrl;
			final String botName = parent.getDiscord().getGuild().getSelfMember().getEffectiveName();

			if (sender instanceof Player player)
			{
				avatarUrl = DiscordUtil.getAvatarUrl(parent.getDiscord(), player);

				final String displayName = FUtil.plainText(player.displayName());
				final String prefix = FormatUtil.stripEssentialsFormat(parent.essentials.getPermissionsHandler().getPrefix(player));
				final String suffix = FormatUtil.stripEssentialsFormat(parent.essentials.getPermissionsHandler().getSuffix(player));
				final String world = parent.essentials.getSettings().getWorldAlias(player.getWorld().getName());

				final String toDiscord = MessageUtil.formatMessage(settings.getMcToDiscordFormat(player, ChatType.UNKNOWN),
						MessageUtil.sanitizeDiscordMarkdown(player.getName()),
						MessageUtil.sanitizeDiscordMarkdown(displayName),
						player.hasPermission("essentials.discord.markdown") ?
								message :
								MessageUtil.sanitizeDiscordMarkdown(message),
						MessageUtil.sanitizeDiscordMarkdown(world),
						MessageUtil.sanitizeDiscordMarkdown(prefix),
						MessageUtil.sanitizeDiscordMarkdown(suffix));

				final String formattedName = MessageUtil.formatMessage(settings.getMcToDiscordNameFormat(player),
						player.getName(),
						displayName,
						world,
						prefix,
						suffix,
						botName);

				DiscordUtil.dispatchDiscordMessage(parent.getDiscord(), type, toDiscord,
						player.hasPermission("essentials.discord.ping"), avatarUrl, formattedName, player.getUniqueId());
			}
			else
			{
				avatarUrl = parent.getDiscord().getGuild().getSelfMember().getEffectiveAvatarUrl();

				final String name = sender != null ? sender.getName() : event.getSenderName();
				final String displayName = sender != null ? sender.getName() : FUtil.plainText(event.getSenderDisplay());

				if (parent.getDirectSettings() == null)
				{
					return;
				}

				final MessageFormat messageFormat = parent.generateMessageFormat(settings,
						parent.getDirectSettings().getString("messages.mc-to-discord", "{displayname}: {message}"),
						"{displayname}: {message}",
						"username",
						"displayname",
						"message",
						"world",
						"prefix",
						"suffix");
				final MessageFormat nameFormat = parent.generateMessageFormat(settings,
						parent.getDirectSettings().getString("messages.mc-to-discord-name-format", "{botname}"),
						"{botname}",
						"username",
						"displayname",
						"world",
						"prefix",
						"suffix",
						"botname");

				final String toDiscord = MessageUtil.formatMessage(Objects.requireNonNull(messageFormat),
						MessageUtil.sanitizeDiscordMarkdown(name),
						MessageUtil.sanitizeDiscordMarkdown(displayName),
						sender != null ?
								sender.hasPermission("essentials.discord.markdown") ?
										message :
										MessageUtil.sanitizeDiscordMarkdown(message) :
								message,
						"",
						"",
						"");

				final String formattedName = MessageUtil.formatMessage(Objects.requireNonNull(nameFormat),
						MessageUtil.sanitizeDiscordMarkdown(name),
						MessageUtil.sanitizeDiscordMarkdown(displayName),
						"",
						"",
						"",
						botName);


				final DiscordMessageEvent discordEvent = new DiscordMessageEvent(type, FormatUtil.stripFormat(toDiscord),
						sender != null && sender.hasPermission("essentials.discord.ping"), avatarUrl,
						FormatUtil.stripFormat(formattedName), null);

				if (Bukkit.isPrimaryThread())
				{
					discordEvent.callEvent();
				}
				else
				{
					Bukkit.getScheduler().runTask(KoolSMPCore.getInstance(), discordEvent::callEvent);
				}
			}
		}

		public void register()
		{
			parent.getDiscord().registerMessageType(KoolSMPCore.getInstance(), type);
			parent.getDiscord().getJda().addEventListener(this);
			Bukkit.getPluginManager().registerEvents(this, KoolSMPCore.getInstance());
		}

		@Override
		public void onMessageReceived(@NotNull MessageReceivedEvent event)
		{
			if (parent.channelDoesNotMatch("adminchat", event.getChannel().getId())
					|| event.getAuthor().equals(parent.getDiscord().getJda().getSelfUser()) || event.getMember() == null)
			{
				return;
			}

			final GroupManagement.Group fallback = KoolSMPCore.getInstance().getGroupManager().getGroupByNameOr("discord", discordGroup);

			final Member member = event.getMember();
			final Component message = Component.text(event.getMessage().getContentDisplay());
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
				final Component replyComponent = Component.text(replyMessage.getContentDisplay());
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
						net.kyori.adventure.text.minimessage.tag.resolver.Formatter.booleanChoice("if_has_attachments", !replyMessage.getAttachments().isEmpty()),
						Placeholder.component("attachments", Component.join(JoinConfiguration.newlines(),
								replyMessage.getAttachments().stream().map(attachment -> Component.text(attachment.getUrl())).toList())),
						Placeholder.component("message", replyComponent),
						net.kyori.adventure.text.minimessage.tag.resolver.Formatter.date("date_created", replyMessage.getTimeCreated()),
						net.kyori.adventure.text.minimessage.tag.resolver.Formatter.booleanChoice("if_edited", replyMessage.getTimeEdited() != null),
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
									attachment.getUrl())), parent.key));

			FUtil.asyncAdminChat(displayName, member.getUser().getName(), userGroup, message, parent.key);
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
	}

	private EssentialsConfiguration getDirectSettings()
	{
		try
		{
			// LOL
			final Field field = DiscordSettings.class.getDeclaredField("config");
			field.setAccessible(true);
			return (EssentialsConfiguration) field.get(service.getSettings());
		}
		catch (Exception ex)
		{
			// I lowkey kinda deserved that
			FLog.error("Failed to get Discord settings directly", ex);
			return null;
		}
	}

	private MessageFormat generateMessageFormat(DiscordSettings settings, String content, String defaultStr,
												String... arguments)
	{
		try
		{
			final Method method = DiscordSettings.class.getDeclaredMethod("generateMessageFormat", String.class,
					String.class, boolean.class, String[].class);
			method.setAccessible(true);
			return (MessageFormat) method.invoke(settings, content, defaultStr, false, arguments);
		}
		catch (Exception ex)
		{
			// I lowkey kinda deserved that
			FLog.error("Failed to run generateMessageFormat directly", ex);
			return null;
		}
	}

	private boolean lacksPermission(Member member, String permission)
	{
		final UUID uuid = linkService.getUUID(member.getId());

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

	private boolean channelDoesNotMatch(String name, String channelId)
	{
		return !channelExists(name) || !channelId.equals(service.getChannel(name, false).getId());
	}
}
