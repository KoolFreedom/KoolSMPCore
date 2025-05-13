package eu.koolfreedom.listener;

import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.banning.Ban;
import eu.koolfreedom.command.impl.DoomCommand;
import eu.koolfreedom.command.impl.SmiteCommand;
import eu.koolfreedom.config.ConfigEntry;
import eu.koolfreedom.log.FLog;
import eu.koolfreedom.punishment.Punishment;
import eu.koolfreedom.util.FUtil;
import eu.koolfreedom.util.TimeOffset;
import io.papermc.paper.event.player.AsyncChatEvent;
import lombok.Getter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerKickEvent;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ChatListener implements Listener
{
	private final Set<FilterEntry> filters = new HashSet<>();

	public ChatListener()
	{
		Bukkit.getPluginManager().registerEvents(this, KoolSMPCore.getInstance());
		loadFilters();
	}

	public void loadFilters()
	{
		if (!filters.isEmpty())
		{
			filters.clear();
		}

		ConfigurationSection section = ConfigEntry.CHAT_FILTER_CATEGORIES.getConfigurationSection();
		section.getKeys(false).stream().filter(section::isConfigurationSection).forEach(key ->
				filters.add(FilterEntry.fromConfigurationSection(key, Objects.requireNonNull(section.getConfigurationSection(key)))));
	}

	public Optional<FilterEntry> getFilterEntryIfApplicable(OfflinePlayer player, String message)
	{
		return filters.stream().filter(entry -> entry.isApplicable(player, message)).findAny();
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	@SuppressWarnings("deprecation")
	private void silentlyCancelDeprecatedChatEventIfCalled(AsyncPlayerChatEvent event)
	{
		// This is here because Paper's developers decided that AsyncChatEvent being cancelled does not influence the
		//  outcome of AsyncPlayerChatEvent. We cancel AsyncChatEvent, but AsyncPlayerChatEvent still gets called, which
		//  is incredibly annoying when you realize that Paper then deprecated APCE, despite you still needing to catch
		//  it. So, you're inevitably going to get warnings about deprecation that are *not* your fault.
		if (ConfigEntry.CHAT_FILTER_ENABLED.getBoolean())
		{
			getFilterEntryIfApplicable(event.getPlayer(), event.getMessage()).ifPresent(filter ->
			{
				if (filter.isCancel())
				{
					event.setCancelled(true);
				}
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
				{
					event.setCancelled(true);
				}

				Bukkit.getScheduler().runTaskLater(KoolSMPCore.getInstance(), () ->
				{
					switch (filter.getAction())
					{
						case DOOM:
						{
							// Only doom them if they are online, otherwise we just fall back to just banning them
							if (player.isOnline())
							{
								DoomCommand.eviscerate(player, Bukkit.getConsoleSender(), filter.getReason());
								break;
							}
						}
						case SMITE:
						{
							if (player.isOnline())
							{
								SmiteCommand.zap(player, Bukkit.getConsoleSender(), filter.getReason());
								break;
							}
						}
						case BAN:
						{
							Ban ban = Ban.fromPlayer(player, Bukkit.getConsoleSender().getName(), filter.getReason(), TimeOffset.getOffset("1d"));
							KoolSMPCore.getInstance().getBanManager().addBan(ban);
							KoolSMPCore.getInstance().getRecordKeeper().recordPunishment(Punishment.fromBan(ban));
							player.kick(ban.getKickMessage(), PlayerKickEvent.Cause.BANNED);
							Bukkit.getOnlinePlayers().stream().filter(suspect -> FUtil.getIp(player).equalsIgnoreCase(FUtil.getIp(player))).forEach(suspect ->
									player.kick(ban.getKickMessage()));
							break;
						}
						case LOG:
						{
							FLog.warning(filter.getReason(), player.getName(), message);
							break;
						}
						case MUTE:
						{
							MuteManager muteManager = KoolSMPCore.getInstance().getMuteManager();

							if (!muteManager.isMuted(player))
							{
								muteManager.setMuted(player, true);
								FUtil.staffAction(Bukkit.getConsoleSender(), "Muted <player>",
										Placeholder.unparsed("player", player.getName()));
								KoolSMPCore.getInstance().getRecordKeeper().recordPunishment(Punishment.builder()
										.uuid(player.getUniqueId())
										.name(player.getName())
										.ip(player.isOnline() ? FUtil.getIp(player) : null)
										.by(Bukkit.getConsoleSender().getName())
										.type("MUTE")
										.build());
							}
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

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	private void allowPinging(AsyncChatEvent event)
	{
		final Player player = event.getPlayer();
		final String message = event.signedMessage().message();

		// In-game pinging
		if (message.contains("@"))
		{
			Arrays.stream(message.split(" ")).filter(word -> word.startsWith("@")).filter(word ->
							!word.equalsIgnoreCase("@everyone") || player.hasPermission("kfc.ping_everyone"))
					.map(ping -> ping.replaceAll("@", "")).map(Bukkit::getPlayer)
					.filter(Objects::nonNull).distinct().forEach(target -> target.playSound(target.getLocation(),
							Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.0F));
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
