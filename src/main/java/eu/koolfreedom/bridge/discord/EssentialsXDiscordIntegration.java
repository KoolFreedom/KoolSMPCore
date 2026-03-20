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
		try
		{
			final Method method = JDADiscordService.class.getMethod("getChannel", String.class, boolean.class);
			return method.invoke(service, name, false) != null;
		}
		catch (Exception ex)
		{
			return false;
		}
	}

	// ---------------------------------------------------------------------------
	// Reflection helpers
	// ---------------------------------------------------------------------------

	Object getRawJda()
	{
		try
		{
			return JDADiscordService.class.getMethod("getJda").invoke(service);
		}
		catch (Exception ex)
		{
			FLog.error("Failed to get raw JDA instance", ex);
			return null;
		}
	}

	String getBotName()
	{
		try
		{
			final Method getGuild = JDADiscordService.class.getMethod("getGuild");
			final Object guild = getGuild.invoke(service);
			final Object selfMember = guild.getClass().getMethod("getSelfMember").invoke(guild);
			return (String) selfMember.getClass().getMethod("getEffectiveName").invoke(selfMember);
		}
		catch (Exception ex)
		{
			FLog.error("Failed to get bot name", ex);
			return "Bot";
		}
	}

	String getBotAvatarUrl()
	{
		try
		{
			final Method getGuild = JDADiscordService.class.getMethod("getGuild");
			final Object guild = getGuild.invoke(service);
			final Object selfMember = guild.getClass().getMethod("getSelfMember").invoke(guild);
			return (String) selfMember.getClass().getMethod("getEffectiveAvatarUrl").invoke(selfMember);
		}
		catch (Exception ex)
		{
			FLog.error("Failed to get bot avatar URL", ex);
			return "";
		}
	}

	void registerJdaListener(JdaEventListener listener)
	{
		try
		{
			final Object jda = getRawJda();
			if (jda == null) return;

			final ClassLoader esxCl = jda.getClass().getClassLoader();
			final Class<?> eventListenerClass = esxCl.loadClass(
					"net.essentialsx.dep.net.dv8tion.jda.api.hooks.EventListener");

			final Object proxy = java.lang.reflect.Proxy.newProxyInstance(
					esxCl,
					new Class[]{ eventListenerClass },
					(proxyObj, method, args) ->
					{
						if ("onEvent".equals(method.getName()) && args != null && args.length == 1)
						{
							final String eventClassName = args[0].getClass().getSimpleName();
							switch (eventClassName)
							{
								case "ButtonInteractionEvent" -> listener.onButtonInteraction(args[0]);
								case "MessageReceivedEvent" -> listener.onMessageReceived(args[0]);
							}
						}
						return null;
					});

			jda.getClass().getMethod("addEventListener", Object[].class)
					.invoke(jda, (Object) new Object[]{ proxy });
		}
		catch (Exception ex)
		{
			FLog.error("Failed to register JDA listener via proxy", ex);
		}
	}

	Object getRawTextChannel(String channelName)
	{
		try
		{
			if (!channelExists(channelName)) return null;
			final Method getChannel = JDADiscordService.class.getMethod("getChannel", String.class, boolean.class);
			final Object channel = getChannel.invoke(service, channelName, false);
			final String channelId = (String) channel.getClass().getMethod("getId").invoke(channel);
			final Object jda = getRawJda();
			if (jda == null) return null;
			return jda.getClass().getMethod("getTextChannelById", String.class).invoke(jda, channelId);
		}
		catch (Exception ex)
		{
			FLog.error("Failed to get text channel: " + channelName, ex);
			return null;
		}
	}

	Object retrieveMessage(Object channel, String messageId)
	{
		try
		{
			return channel.getClass().getMethod("retrieveMessageById", String.class)
					.invoke(channel, messageId);
		}
		catch (Exception ex)
		{
			FLog.error("Failed to retrieve message by ID", ex);
			return null;
		}
	}

	void queueAction(Object restAction, java.util.function.Consumer<Object> onSuccess,
					 java.util.function.Consumer<Throwable> onFailure)
	{
		try
		{
			if (onSuccess != null && onFailure != null)
			{
				restAction.getClass().getMethod("queue",
								java.util.function.Consumer.class,
								java.util.function.Consumer.class)
						.invoke(restAction, onSuccess, onFailure);
			}
			else if (onSuccess != null)
			{
				restAction.getClass().getMethod("queue", java.util.function.Consumer.class)
						.invoke(restAction, onSuccess);
			}
			else
			{
				restAction.getClass().getMethod("queue").invoke(restAction);
			}
		}
		catch (Exception ex)
		{
			FLog.error("Failed to queue RestAction", ex);
		}
	}

	void sendReportMessage(String channelName, String content,
						   Object embed, List<Object> buttons,
						   java.util.function.Consumer<Object> onSuccess)
	{
		try
		{
			final Object channel = getRawTextChannel(channelName);
			if (channel == null) return;

			Object action;
			if (content == null)
			{
				final Object emptyArray = java.lang.reflect.Array.newInstance(embed.getClass(), 0);
				action = findMethod(channel.getClass(), "sendMessageEmbeds")
						.invoke(channel, embed, emptyArray);
			}
			else
			{
				action = channel.getClass().getMethod("sendMessage", CharSequence.class)
						.invoke(channel, content);
				if (embed != null)
				{
					action = findMethod(action.getClass(), "setEmbeds").invoke(action, List.of(embed));
				}
			}

			if (buttons != null && !buttons.isEmpty())
			{
				action = findMethod(action.getClass(), "setActionRow")
						.invoke(action, (Object) buttons.toArray());
			}

			queueAction(action, onSuccess, null);
		}
		catch (Exception ex)
		{
			FLog.error("Failed to send report message", ex);
		}
	}

	void editReportMessage(Object message, String content, Object embed, List<Object> buttons,
						   Runnable onFailure)
	{
		try
		{
			Object action = message.getClass().getMethod("editMessage", CharSequence.class)
					.invoke(message, content);

			action = findMethod(action.getClass(), "setEmbeds").invoke(action, List.of(embed));

			if (buttons != null && !buttons.isEmpty())
			{
				action = findMethod(action.getClass(), "setActionRow")
						.invoke(action, (Object) buttons.toArray());
			}

			queueAction(action, null, err -> { if (onFailure != null) onFailure.run(); });
		}
		catch (Exception ex)
		{
			FLog.error("Failed to edit report message", ex);
			if (onFailure != null) onFailure.run();
		}
	}

	void purgeChannelMessages(String channelName, List<String> messageIds)
	{
		try
		{
			final Object channel = getRawTextChannel(channelName);
			if (channel == null) return;
			channel.getClass().getMethod("purgeMessagesById", List.class).invoke(channel, messageIds);
		}
		catch (Exception ex)
		{
			FLog.error("Failed to purge channel messages", ex);
		}
	}

	String getAvatarUrl(Player player)
	{
		try
		{
			final Method method = DiscordUtil.class.getDeclaredMethod("getAvatarUrl",
					JDADiscordService.class, Player.class);
			method.setAccessible(true);
			return (String) method.invoke(null, service, player);
		}
		catch (Exception ex)
		{
			FLog.error("Failed to get avatar URL via reflection", ex);
			return getBotAvatarUrl();
		}
	}

	void dispatchDiscordMessage(MessageType type, String message, boolean ping,
								String avatarUrl, String name, UUID uuid)
	{
		try
		{
			final Method method = DiscordUtil.class.getDeclaredMethod("dispatchDiscordMessage",
					JDADiscordService.class, MessageType.class, String.class,
					boolean.class, String.class, String.class, UUID.class);
			method.setAccessible(true);
			method.invoke(null, service, type, message, ping, avatarUrl, name, uuid);
		}
		catch (Exception ex)
		{
			FLog.error("Failed to dispatch Discord message via reflection", ex);
		}
	}

	private Method findMethod(Class<?> clazz, String name)
	{
		for (Method m : clazz.getMethods())
		{
			if (m.getName().equals(name)) return m;
		}
		throw new RuntimeException("Method not found: " + name + " on " + clazz.getName());
	}

	interface JdaEventListener
	{
		default void onButtonInteraction(Object event) {}
		default void onMessageReceived(Object event) {}
	}

	// ---------------------------------------------------------------------------
	// Inner classes
	// ---------------------------------------------------------------------------

	@Getter
	@RequiredArgsConstructor
	public static class ReportsMessageType implements Listener, JdaEventListener
	{
		private final EssentialsXDiscordIntegration parent;
		private final MessageType type = new MessageType("reports");

		@Override
		public void onButtonInteraction(@NotNull Object rawEvent)
		{
			try
			{
				final Object channel = rawEvent.getClass().getMethod("getChannel").invoke(rawEvent);
				final String channelId = (String) channel.getClass().getMethod("getId").invoke(channel);
				if (parent.channelDoesNotMatch("reports", channelId)) return;

				final Object member = rawEvent.getClass().getMethod("getMember").invoke(rawEvent);
				if (member == null) return;

				final Object message = rawEvent.getClass().getMethod("getMessage").invoke(rawEvent);
				final Object button = rawEvent.getClass().getMethod("getButton").invoke(rawEvent);
				final String buttonId = (String) button.getClass().getMethod("getId").invoke(button);
				if (buttonId == null) return;

				final String messageId = (String) message.getClass().getMethod("getId").invoke(message);
				final String memberMention = (String) member.getClass().getMethod("getAsMention").invoke(member);
				final Object memberUser = member.getClass().getMethod("getUser").invoke(member);
				final String memberUserName = (String) memberUser.getClass().getMethod("getName").invoke(memberUser);
				final String memberUserId = (String) memberUser.getClass().getMethod("getId").invoke(memberUser);

				final Object messageAuthor = message.getClass().getMethod("getAuthor").invoke(message);
				final Object jda = parent.getRawJda();
				if (jda == null) return;
				final Object selfUser = jda.getClass().getMethod("getSelfUser").invoke(jda);
				if (!messageAuthor.equals(selfUser)) return;

				final Component userDisplay = Component.text(memberMention);

				final Optional<Report> optionalReport = KoolSMPCore.getInstance().getReportManager()
						.getReports(true).stream()
						.filter(report -> report.getAdditionalData()
								.getString("discordMessageId", "-1").equalsIgnoreCase(messageId))
						.findAny();

				final java.util.function.BiConsumer<String, Boolean> reply = (text, ephemeral) ->
				{
					try
					{
						Object replyAction = rawEvent.getClass()
								.getMethod("reply", String.class).invoke(rawEvent, text);
						replyAction = replyAction.getClass()
								.getMethod("setEphemeral", boolean.class).invoke(replyAction, ephemeral);
						replyAction.getClass().getMethod("queue").invoke(replyAction);
					}
					catch (Exception ex) { FLog.error("Failed to send ephemeral reply", ex); }
				};

				switch (buttonId.toLowerCase())
				{
					case "handled" ->
					{
						if (parent.lacksPermission(member, "kfc.command.reports.handle"))
						{
							reply.accept("You don't have permission to do that.", true);
							return;
						}
						optionalReport.ifPresentOrElse(report ->
						{
							if (report.isResolved())
							{
								reply.accept("This report has already been resolved.", true);
								return;
							}
							report.updateAsync(userDisplay, memberUserName, memberUserId,
									Report.ReportStatus.RESOLVED, "Handled");
							reply.accept("Report " + report.getId() + " has been marked as handled.", true);
						}, () ->
						{
							try
							{
								Object editAction = message.getClass()
										.getMethod("editMessage", CharSequence.class)
										.invoke(message, "Unlinked report resolved by " + memberMention);
								final Object embeds = message.getClass().getMethod("getEmbeds").invoke(message);
								editAction = editAction.getClass()
										.getMethod("setEmbeds", Collection.class).invoke(editAction, embeds);
								editAction.getClass().getMethod("queue").invoke(editAction);
							}
							catch (Exception ex) { FLog.error("Failed to edit message", ex); }
						});
					}
					case "invalid" ->
					{
						if (parent.lacksPermission(member, "kfc.command.reports.close"))
						{
							reply.accept("You don't have permission to do that.", true);
							return;
						}
						optionalReport.ifPresentOrElse(report ->
						{
							if (report.isResolved())
							{
								reply.accept("This report has already been resolved.", true);
								return;
							}
							report.updateAsync(userDisplay, memberUserName, memberUserId,
									Report.ReportStatus.CLOSED, "Invalid");
							reply.accept("Report " + report.getId() + " has been closed.", true);
						}, () ->
						{
							try
							{
								message.getClass().getMethod("delete").invoke(message);
								reply.accept("This report didn't even have a message ID associated to it somehow, so we deleted it.", true);
							}
							catch (Exception ex) { FLog.error("Failed to delete message", ex); }
						});
					}
					case "purge" ->
					{
						if (parent.lacksPermission(member, "kfc.command.reports.purge"))
						{
							reply.accept("You don't have permission to do that.", true);
							return;
						}
						optionalReport.ifPresentOrElse(report ->
						{
							KoolSMPCore.getInstance().getReportManager().deleteReportsByUuidAsync(
									userDisplay, memberUserName, memberUserId, report.getReporter());
							reply.accept("Reports are now being deleted as we speak.", true);
						}, () ->
						{
							try
							{
								message.getClass().getMethod("delete").invoke(message);
								reply.accept("This report didn't even have a message ID associated to it somehow, so we couldn't delete every report this user filed.", true);
							}
							catch (Exception ex) { FLog.error("Failed to delete message", ex); }
						});
					}
					case "reopen" ->
					{
						if (parent.lacksPermission(member, "kfc.command.reports.reopen"))
						{
							reply.accept("You don't have permission to do that.", true);
							return;
						}
						optionalReport.ifPresentOrElse(report ->
						{
							if (!report.isResolved())
							{
								reply.accept("This report is already open.", true);
								return;
							}
							report.updateAsync(userDisplay, memberUserName, memberUserId,
									Report.ReportStatus.REOPENED, "Reopened for further investigation");
							reply.accept("Report " + report.getId() + " has been re-opened.", true);
						}, () -> reply.accept("This report doesn't have a message ID associated to it, so we can't re-open it.", true));
					}
				}
			}
			catch (Exception ex)
			{
				FLog.error("Error handling button interaction", ex);
			}
		}

		@EventHandler
		private void onReport(PlayerReportEvent event)
		{
			if (!getParent().channelExists("reports")) return;

			final Report report = event.getReport();
			final Object embed = createEmbedFromReport(report);
			final List<Object> buttons = createButtonsFromReport(report);

			getParent().sendReportMessage("reports", null, embed, buttons, message ->
			{
				try
				{
					final String id = (String) message.getClass().getMethod("getId").invoke(message);
					report.getAdditionalData().set("discordMessageId", id);
					CompletableFuture.runAsync(() -> KoolSMPCore.getInstance().getReportManager().save());
				}
				catch (Exception ex) { FLog.error("Failed to store report message ID", ex); }
			});
		}

		@EventHandler
		private void onReportUpdated(PlayerReportUpdateEvent event)
		{
			if (!getParent().channelExists("reports")) return;

			final Report report = event.getReport();
			final Object channel = getParent().getRawTextChannel("reports");
			if (channel == null) return;

			final AtomicBoolean createMessage = new AtomicBoolean(false);
			final Object embed = createEmbedFromReport(report);
			final List<Object> buttons = createButtonsFromReport(report);
			final String staffDisplay = FUtil.plainText(event.getStaffDisplayName());

			if (!report.getAdditionalData().isString("discordMessageId"))
			{
				createMessage.set(true);
			}
			else
			{
				final Object retrieveAction = getParent().retrieveMessage(channel,
						report.getAdditionalData().getString("discordMessageId"));
				if (retrieveAction != null)
				{
					getParent().queueAction(retrieveAction,
							message -> getParent().editReportMessage(message,
									"Report updated by " + staffDisplay, embed, buttons,
									() -> createMessage.set(true)),
							err -> createMessage.set(true));
				}
				else
				{
					createMessage.set(true);
				}
			}

			if (createMessage.get())
			{
				getParent().sendReportMessage("reports", "Report updated by " + event.getStaffName(),
						embed, buttons, success ->
						{
							try
							{
								final String id = (String) success.getClass().getMethod("getId").invoke(success);
								report.getAdditionalData().set("discordMessageId", id);
								CompletableFuture.runAsync(() -> KoolSMPCore.getInstance().getReportManager().save());
							}
							catch (Exception ex) { FLog.error("Failed to store updated report message ID", ex); }
						});
			}
		}

		@EventHandler
		private void onReportDeleted(PlayerReportDeleteEvent event)
		{
			if (!getParent().channelExists("reports")) return;

			getParent().purgeChannelMessages("reports",
					event.getReports().stream()
							.filter(r -> r.getAdditionalData().isString("discordMessageId"))
							.map(r -> r.getAdditionalData().getString("discordMessageId"))
							.toList());
		}

		public void register()
		{
			Bukkit.getPluginManager().registerEvents(this, KoolSMPCore.getInstance());
			getParent().registerJdaListener(this);
		}

		private Object createEmbedFromReport(Report report)
		{
			try
			{
				final Object jda = parent.getRawJda();
				if (jda == null) return null;
				final ClassLoader cl = jda.getClass().getClassLoader();
				final Class<?> embedBuilderClass = cl.loadClass(
						"net.essentialsx.dep.net.dv8tion.jda.api.EmbedBuilder");
				final Object builder = embedBuilderClass.getConstructor().newInstance();

				final OfflinePlayer reporter = Bukkit.getOfflinePlayer(report.getReporter());
				final OfflinePlayer reported = Bukkit.getOfflinePlayer(report.getReported());

				final String title = (report.getStatus() != Report.ReportStatus.UNRESOLVED
						? "**" + report.getStatus().name() + "**: " : "")
						+ "Report for " + reported.getName()
						+ (!reported.isOnline() ? " (offline)" : "");

				embedBuilderClass.getMethod("setTitle", String.class, String.class)
						.invoke(builder, title, null);
				embedBuilderClass.getMethod("setDescription", CharSequence.class)
						.invoke(builder, report.getReason());
				embedBuilderClass.getMethod("setColor", int.class)
						.invoke(builder, report.getStatus().getAwtColor().getRGB());
				embedBuilderClass.getMethod("setFooter", String.class, String.class)
						.invoke(builder,
								"ID " + report.getId() + "  • Reported by " + reporter.getName(),
								"https://minotar.net/helm/" + reporter.getName() + ".png");

				final ZonedDateTime zdt = ZonedDateTime.of(
						LocalDateTime.ofEpochSecond(report.getTimestamp(), 0,
								ZoneId.systemDefault().getRules().getOffset(Instant.now())),
						ZoneId.systemDefault());
				embedBuilderClass.getMethod("setTimestamp", java.time.temporal.TemporalAccessor.class)
						.invoke(builder, zdt);

				if (report.getLastNote() != null)
				{
					embedBuilderClass.getMethod("addField", String.class, String.class, boolean.class)
							.invoke(builder, "Staff Note", report.getLastNote(), true);
				}

				return embedBuilderClass.getMethod("build").invoke(builder);
			}
			catch (Exception ex)
			{
				FLog.error("Failed to create report embed", ex);
				return null;
			}
		}

		private List<Object> createButtonsFromReport(Report report)
		{
			try
			{
				final Object jda = parent.getRawJda();
				if (jda == null) return List.of();
				final ClassLoader cl = jda.getClass().getClassLoader();
				final Class<?> buttonClass = cl.loadClass(
						"net.essentialsx.dep.net.dv8tion.jda.api.interactions.components.buttons.Button");

				if (report.isResolved())
				{
					return List.of(buttonClass.getMethod("primary", String.class, String.class)
							.invoke(null, "reopen", "Re-open"));
				}
				else
				{
					return List.of(
							buttonClass.getMethod("primary", String.class, String.class)
									.invoke(null, "handled", "Resolve"),
							buttonClass.getMethod("primary", String.class, String.class)
									.invoke(null, "invalid", "Invalid"),
							buttonClass.getMethod("danger", String.class, String.class)
									.invoke(null, "purge", "Purge"));
				}
			}
			catch (Exception ex)
			{
				FLog.error("Failed to create report buttons", ex);
				return List.of();
			}
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
	public static class AdminChatType implements Listener, JdaEventListener
	{
		private final EssentialsXDiscordIntegration parent;
		private final MessageType type = new MessageType("adminchat");
		private static final Map<String, GroupManagement.Group> roleMap = new HashMap<>();
		private static final GroupManagement.Group discordGroup = GroupManagement.Group.createGroup(
				"discord", "Discord",
				Component.text("Discord").color(TextColor.color(0x5865F2)),
				TextColor.color(0x5865F2));

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
			final String botName = parent.getBotName();

			if (sender instanceof Player player)
			{
				avatarUrl = parent.getAvatarUrl(player);

				final String displayName = FUtil.plainText(player.displayName());
				final String prefix = FormatUtil.stripEssentialsFormat(
						parent.essentials.getPermissionsHandler().getPrefix(player));
				final String suffix = FormatUtil.stripEssentialsFormat(
						parent.essentials.getPermissionsHandler().getSuffix(player));
				final String world = parent.essentials.getSettings().getWorldAlias(player.getWorld().getName());

				final String toDiscord = MessageUtil.formatMessage(
						settings.getMcToDiscordFormat(player, ChatType.UNKNOWN),
						MessageUtil.sanitizeDiscordMarkdown(player.getName()),
						MessageUtil.sanitizeDiscordMarkdown(displayName),
						player.hasPermission("essentials.discord.markdown") ?
								message : MessageUtil.sanitizeDiscordMarkdown(message),
						MessageUtil.sanitizeDiscordMarkdown(world),
						MessageUtil.sanitizeDiscordMarkdown(prefix),
						MessageUtil.sanitizeDiscordMarkdown(suffix));

				final String formattedName = MessageUtil.formatMessage(
						settings.getMcToDiscordNameFormat(player),
						player.getName(), displayName, world, prefix, suffix, botName);

				parent.dispatchDiscordMessage(type, toDiscord,
						player.hasPermission("essentials.discord.ping"), avatarUrl,
						formattedName, player.getUniqueId());
			}
			else
			{
				avatarUrl = parent.getBotAvatarUrl();

				final String name = sender != null ? sender.getName() : event.getSenderName();
				final String displayName = sender != null ? sender.getName() :
						FUtil.plainText(event.getSenderDisplay());

				if (parent.getDirectSettings() == null) return;

				final MessageFormat messageFormat = parent.generateMessageFormat(settings,
						parent.getDirectSettings().getString("messages.mc-to-discord",
								"{displayname}: {message}"),
						"{displayname}: {message}",
						"username", "displayname", "message", "world", "prefix", "suffix");
				final MessageFormat nameFormat = parent.generateMessageFormat(settings,
						parent.getDirectSettings().getString("messages.mc-to-discord-name-format",
								"{botname}"),
						"{botname}",
						"username", "displayname", "world", "prefix", "suffix", "botname");

				final String toDiscord = MessageUtil.formatMessage(
						Objects.requireNonNull(messageFormat),
						MessageUtil.sanitizeDiscordMarkdown(name),
						MessageUtil.sanitizeDiscordMarkdown(displayName),
						sender != null ?
								sender.hasPermission("essentials.discord.markdown") ?
										message : MessageUtil.sanitizeDiscordMarkdown(message) :
								message,
						"", "", "");

				final String formattedName = MessageUtil.formatMessage(
						Objects.requireNonNull(nameFormat),
						MessageUtil.sanitizeDiscordMarkdown(name),
						MessageUtil.sanitizeDiscordMarkdown(displayName),
						"", "", "", botName);

				final DiscordMessageEvent discordEvent = new DiscordMessageEvent(type,
						FormatUtil.stripFormat(toDiscord),
						sender != null && sender.hasPermission("essentials.discord.ping"),
						avatarUrl, FormatUtil.stripFormat(formattedName), null);

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
			parent.registerJdaListener(this);
			Bukkit.getPluginManager().registerEvents(this, KoolSMPCore.getInstance());
		}

		@Override
		public void onMessageReceived(@NotNull Object rawEvent)
		{
			try
			{
				final Object channel = rawEvent.getClass().getMethod("getChannel").invoke(rawEvent);
				final String channelId = (String) channel.getClass().getMethod("getId").invoke(channel);
				if (parent.channelDoesNotMatch("adminchat", channelId)) return;

				final Object author = rawEvent.getClass().getMethod("getAuthor").invoke(rawEvent);
				final Object jda = parent.getRawJda();
				if (jda == null) return;
				final Object selfUser = jda.getClass().getMethod("getSelfUser").invoke(jda);
				if (author.equals(selfUser)) return;

				final Object member = rawEvent.getClass().getMethod("getMember").invoke(rawEvent);
				if (member == null) return;

				final Object jdaMessage = rawEvent.getClass().getMethod("getMessage").invoke(rawEvent);
				final String contentDisplay = (String) jdaMessage.getClass()
						.getMethod("getContentDisplay").invoke(jdaMessage);

				final GroupManagement.Group fallback = KoolSMPCore.getInstance().getGroupManager()
						.getGroupByNameOr("discord", discordGroup);

				@SuppressWarnings("unchecked")
				final List<Object> roles = (List<Object>) member.getClass().getMethod("getRoles").invoke(member);

				final GroupManagement.Group userGroup = switch (ConfigEntry.DISCORD_GROUP_MODE_SWITCH.getInteger())
				{
					case 1 -> KoolSMPCore.getInstance().getGroupManager().getGroupByNameOr(
							roles.isEmpty() ? "discord" :
									(String) roles.getFirst().getClass().getMethod("getName").invoke(roles.getFirst()),
							!roles.isEmpty() ? getGroupFromRole(roles.getFirst()) : fallback);
					case 2 -> !roles.isEmpty() ? getGroupFromRole(roles.getFirst()) : fallback;
					default -> fallback;
				};

				Component reply = Component.empty();
				final Object referencedMessage = jdaMessage.getClass()
						.getMethod("getReferencedMessage").invoke(jdaMessage);

				if (referencedMessage != null)
				{
					final String replyContent = (String) referencedMessage.getClass()
							.getMethod("getContentDisplay").invoke(referencedMessage);
					final Object replyMemberObj = referencedMessage.getClass()
							.getMethod("getMember").invoke(referencedMessage);
					final Object replyAuthor = referencedMessage.getClass()
							.getMethod("getAuthor").invoke(referencedMessage);

					final String replyMessageId = (String) referencedMessage.getClass()
							.getMethod("getId").invoke(referencedMessage);
					final String replyUserId = (String) replyAuthor.getClass()
							.getMethod("getId").invoke(replyAuthor);
					final String replyUsername = (String) replyAuthor.getClass()
							.getMethod("getName").invoke(replyAuthor);
					final String replyEffectiveName = (String) replyAuthor.getClass()
							.getMethod("getEffectiveName").invoke(replyAuthor);
					final String replyNickname = replyMemberObj != null
							? (String) replyMemberObj.getClass().getMethod("getEffectiveName").invoke(replyMemberObj)
							: replyEffectiveName;
					final int replyColorRaw = replyMemberObj != null
							? (int) replyMemberObj.getClass().getMethod("getColorRaw").invoke(replyMemberObj)
							: 0xFFFFFF;

					@SuppressWarnings("unchecked")
					final List<Object> replyAttachments = (List<Object>) referencedMessage.getClass()
							.getMethod("getAttachments").invoke(referencedMessage);
					final Object replyTimeCreated = referencedMessage.getClass()
							.getMethod("getTimeCreated").invoke(referencedMessage);
					final Object replyTimeEdited = referencedMessage.getClass()
							.getMethod("getTimeEdited").invoke(referencedMessage);

					reply = FUtil.miniMessage(ConfigEntry.DISCORD_REPLYING_TO_FORMAT.getString(),
							Placeholder.parsed("message_id", replyMessageId),
							Placeholder.parsed("user_id", replyUserId),
							Placeholder.parsed("username", replyUsername),
							Placeholder.parsed("name", replyEffectiveName),
							Placeholder.unparsed("nickname", replyNickname),
							replyMemberObj != null
									? Placeholder.styling("role_color",
									b -> b.color(TextColor.color(replyColorRaw)))
									: Placeholder.component("role_color", Component.empty()),
							Formatter.booleanChoice("if_has_attachments", !replyAttachments.isEmpty()),
							Placeholder.component("attachments", Component.join(JoinConfiguration.newlines(),
									replyAttachments.stream().map(a ->
									{
										try
										{
											return Component.text((String) a.getClass()
													.getMethod("getUrl").invoke(a));
										}
										catch (Exception ex) { return Component.empty(); }
									}).toList())),
							Placeholder.component("message", Component.text(replyContent)),
							Formatter.date("date_created",
									(java.time.temporal.TemporalAccessor) replyTimeCreated),
							Formatter.booleanChoice("if_edited", replyTimeEdited != null),
							replyTimeEdited != null
									? Formatter.date("date_edited",
									(java.time.temporal.TemporalAccessor) replyTimeEdited)
									: Placeholder.component("date_edited", Component.empty()),
							Placeholder.component("roles", buildRolesComponent(roles)));
				}

				final String memberId = (String) member.getClass().getMethod("getId").invoke(member);
				final Object memberUser = member.getClass().getMethod("getUser").invoke(member);
				final String memberUsername = (String) memberUser.getClass()
						.getMethod("getName").invoke(memberUser);
				final String memberEffectiveName = (String) memberUser.getClass()
						.getMethod("getEffectiveName").invoke(memberUser);
				final String memberNickname = (String) member.getClass()
						.getMethod("getEffectiveName").invoke(member);
				final int memberColorRaw = (int) member.getClass()
						.getMethod("getColorRaw").invoke(member);

				final Component displayName = FUtil.miniMessage(ConfigEntry.DISCORD_USER_FORMAT.getString(),
						Placeholder.parsed("id", memberId),
						Placeholder.parsed("username", memberUsername),
						Placeholder.parsed("name", memberEffectiveName),
						Placeholder.unparsed("nickname", memberNickname),
						Placeholder.styling("role_color", b -> b.color(TextColor.color(memberColorRaw))),
						Placeholder.component("roles", buildRolesComponent(roles)),
						Placeholder.component("reply", reply));

				@SuppressWarnings("unchecked")
				final List<Object> attachments = (List<Object>) jdaMessage.getClass()
						.getMethod("getAttachments").invoke(jdaMessage);

				attachments.forEach(attachment ->
				{
					try
					{
						final String url = (String) attachment.getClass()
								.getMethod("getUrl").invoke(attachment);
						FUtil.asyncAdminChat(displayName, memberUsername, userGroup,
								Component.text(url).clickEvent(
										ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, url)),
								parent.key);
					}
					catch (Exception ex) { FLog.error("Failed to process attachment", ex); }
				});

				FUtil.asyncAdminChat(displayName, memberUsername, userGroup,
						Component.text(contentDisplay), parent.key);
			}
			catch (Exception ex)
			{
				FLog.error("Error handling Discord message in admin chat", ex);
			}
		}

		private Component buildRolesComponent(List<Object> roles)
		{
			if (roles.isEmpty())
			{
				return Component.text("(none)").color(NamedTextColor.GRAY);
			}
			return Component.text(" - ").color(NamedTextColor.GRAY).append(
					Component.join(JoinConfiguration.separator(
									Component.newline().append(Component.text(" - ").color(NamedTextColor.GRAY))),
							roles.stream().map(role ->
							{
								try
								{
									final String rName = (String) role.getClass()
											.getMethod("getName").invoke(role);
									final int rColor = (int) role.getClass()
											.getMethod("getColorRaw").invoke(role);
									return Component.text(rName).color(TextColor.color(rColor));
								}
								catch (Exception ex) { return Component.empty(); }
							}).toList()));
		}

		private GroupManagement.Group getGroupFromRole(Object role)
		{
			try
			{
				final String roleId = (String) role.getClass().getMethod("getId").invoke(role);
				final String roleName = (String) role.getClass().getMethod("getName").invoke(role);
				final int roleColor = (int) role.getClass().getMethod("getColorRaw").invoke(role);

				if (!roleMap.containsKey(roleId)
						|| roleMap.get(roleId).getColor().value() != roleColor
						|| !roleMap.get(roleId).getName().equalsIgnoreCase(roleName))
				{
					roleMap.remove(roleId);
					final GroupManagement.Group roleGroup = GroupManagement.Group.createGroup(
							roleId, roleName,
							Component.text(roleName).color(TextColor.color(roleColor)),
							TextColor.color(roleColor));
					roleMap.put(roleId, roleGroup);
				}

				return roleMap.get(roleId);
			}
			catch (Exception ex)
			{
				FLog.error("Failed to get group from role", ex);
				return discordGroup;
			}
		}
	}

	private EssentialsConfiguration getDirectSettings()
	{
		try
		{
			final Field field = DiscordSettings.class.getDeclaredField("config");
			field.setAccessible(true);
			return (EssentialsConfiguration) field.get(service.getSettings());
		}
		catch (Exception ex)
		{
			FLog.error("Failed to get Discord settings directly", ex);
			return null;
		}
	}

	private MessageFormat generateMessageFormat(DiscordSettings settings, String content,
												String defaultStr, String... arguments)
	{
		try
		{
			final Method method = DiscordSettings.class.getDeclaredMethod("generateMessageFormat",
					String.class, String.class, boolean.class, String[].class);
			method.setAccessible(true);
			return (MessageFormat) method.invoke(settings, content, defaultStr, false, arguments);
		}
		catch (Exception ex)
		{
			FLog.error("Failed to run generateMessageFormat directly", ex);
			return null;
		}
	}

	private boolean lacksPermission(Object member, String permission)
	{
		try
		{
			final Object memberUser = member.getClass().getMethod("getUser").invoke(member);
			final String discordId = (String) memberUser.getClass().getMethod("getId").invoke(memberUser);
			final UUID uuid = linkService.getUUID(discordId);
			if (uuid == null) return true;

			final OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
			if (!player.isOnline() && !player.hasPlayedBefore()) return true;

			return !KoolSMPCore.getInstance().getGroupManager().getVaultPermissions()
					.playerHas(null, player, permission);
		}
		catch (Exception ex)
		{
			FLog.error("Failed to check permission", ex);
			return true;
		}
	}

	private boolean channelDoesNotMatch(String name, String channelId)
	{
		try
		{
			if (!channelExists(name)) return true;
			final Method method = JDADiscordService.class.getMethod("getChannel", String.class, boolean.class);
			final Object channel = method.invoke(service, name, false);
			final String id = (String) channel.getClass().getMethod("getId").invoke(channel);
			return !channelId.equals(id);
		}
		catch (Exception ex)
		{
			return true;
		}
	}
}