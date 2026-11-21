package eu.koolfreedom.listener.impl;

import eu.koolfreedom.banning.Ban;
import eu.koolfreedom.command.impl.DoomCommand;
import eu.koolfreedom.command.impl.SmiteCommand;
import eu.koolfreedom.config.ConfigEntry;
import eu.koolfreedom.listener.KoolListener;
import eu.koolfreedom.util.FLog;
import eu.koolfreedom.punishment.Punishment;
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
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerKickEvent;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ChatListener extends KoolListener
{
	public ChatListener() {}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void allowPinging(AsyncChatEvent event)
	{
		final Player player = event.getPlayer();
		final String message = event.signedMessage().message();

		final List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
		final Map<String, Player> playersByName = new HashMap<>();
		onlinePlayers.forEach(target -> playersByName.put(target.getName().toLowerCase(Locale.ROOT), target));
		final String playerNames = onlinePlayers.stream()
				.map(Player::getName)
				.sorted(Comparator.comparingInt(String::length).reversed())
				.map(Pattern::quote)
				.reduce((left, right) -> left + "|" + right)
				.orElse("(?!)");
		final Pattern mentionPattern = Pattern.compile(
				"(?iu)(?<![\\p{L}\\p{N}_])(?:@(?<special>everyone|here)|@?(?<name>" + playerNames + "))(?![\\p{L}\\p{N}_])");
		final Matcher matcher = mentionPattern.matcher(message);
		final var highlightedMessage = Component.text();
		final Set<Player> pingedPlayers = new LinkedHashSet<>();
		final boolean canPingEveryone = player.hasPermission("kfc.ping_everyone");
		int lastEnd = 0;
		boolean hasPing = false;

		while (matcher.find())
		{
			final String special = matcher.group("special");
			final Player target = special == null
					? playersByName.get(matcher.group("name").toLowerCase(Locale.ROOT))
					: null;
			if (special != null && !canPingEveryone)
			{
				continue;
			}
			if (special == null && target == null)
			{
				continue;
			}

			highlightedMessage.append(Component.text(message.substring(lastEnd, matcher.start()), NamedTextColor.YELLOW));
			final String renderedMention = special == null ? "@" + target.getName() : matcher.group();
			highlightedMessage.append(Component.text(renderedMention, NamedTextColor.GOLD));
			lastEnd = matcher.end();
			hasPing = true;

			if (special != null)
			{
				pingedPlayers.addAll(onlinePlayers);
			}
			else
			{
				pingedPlayers.add(target);
			}
		}

		if (hasPing)
		{
			highlightedMessage.append(Component.text(message.substring(lastEnd), NamedTextColor.YELLOW));
			final Component pingMessage = highlightedMessage.build();
			event.message(pingMessage);
			event.renderer((source, sourceDisplayName, ignoredMessage, viewer) -> sourceDisplayName
					.append(Component.text(" -> ", NamedTextColor.DARK_GRAY))
					.append(pingMessage));
			Bukkit.getScheduler().runTask(plugin, () -> pingedPlayers.stream().filter(Player::isOnline)
					.forEach(target -> target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.0F)));
		}
	}
}
