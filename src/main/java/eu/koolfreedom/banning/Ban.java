package eu.koolfreedom.banning;

import eu.koolfreedom.config.ConfigEntry;
import eu.koolfreedom.util.FUtil;
import lombok.Builder;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Builder
@Getter
public class Ban
{
	private static final DateTimeFormatter EXPIRATION_FORMAT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd 'at' HH:mm:ss z")
					.withZone(ZoneId.systemDefault());

	private long id;
	@Builder.Default private UUID uuid = null;
	@Builder.Default private String name = null;
	@Builder.Default private String by = null;
	@Builder.Default private String reason = null;
	@Builder.Default private long expires = Long.MAX_VALUE;
	@Builder.Default private List<String> ips = Collections.emptyList();

	/**
	 * Create a ban for an online or offline player (UUID known).
	 */
	public static Ban fromPlayer(OfflinePlayer player, String by, String reason, long duration)
	{
		BanBuilder builder = builder()
				.name(player.getName())
				.uuid(player.getUniqueId());

		if (player instanceof Player online)
			builder.ips(new ArrayList<>(List.of(FUtil.getIp(online))));

		long now = System.currentTimeMillis();
		return builder
				.id(now)
				.by(by)
				.reason(reason)
				.expires(duration != Long.MAX_VALUE ? now + duration : Long.MAX_VALUE)
				.build();
	}

	/**
	 * Create a pre-emptive ban by username only (player has never joined).
	 */
	public static Ban fromUsername(String username, String by, String reason, long duration)
	{
		long now = System.currentTimeMillis();
		return builder()
				.id(now)
				.name(username)
				.by(by)
				.reason(reason)
				.expires(duration != Long.MAX_VALUE ? now + duration : Long.MAX_VALUE)
				.ips(Collections.emptyList())
				.build();
	}

	public boolean canExpire()
	{
		return expires != Long.MAX_VALUE;
	}
	public boolean isExpired()
	{
		return System.currentTimeMillis() >= expires;
	}
	public boolean makesSense()
	{
		return !ips.isEmpty() || name != null || uuid != null;
	}

	public String getDurationString()
	{
		if (!canExpire()) return "permanent";
		long remaining = expires - System.currentTimeMillis();
		if (remaining <= 0) return "expired";
		long secs = remaining / 1000;
		long days = secs / 86400; secs %= 86400;
		long hours = secs / 3600; secs %= 3600;
		long mins = secs / 60;
		StringBuilder sb = new StringBuilder();
		if (days  > 0) sb.append(days).append("d ");
		if (hours > 0) sb.append(hours).append("h ");
		if (mins  > 0) sb.append(mins).append("m");
		return sb.isEmpty() ? "less than a minute" : sb.toString().trim();
	}

	public Component getKickMessage()
	{
		StringBuilder msg = new StringBuilder("<red>You are banned from this server.");
		if (by != null && !by.isBlank())
			msg.append("<newline>Banned by: <yellow><by></yellow>");
		if (reason != null && !reason.isBlank())
			msg.append("<newline>Reason: <yellow><reason></yellow>");
		if (canExpire())
			msg.append("<newline>Expires: <yellow><expires></yellow>");
		msg.append("<newline><red>You can appeal at <yellow>")
				.append(ConfigEntry.SERVER_APPEAL_URL.getString())
				.append("</yellow>");

		return FUtil.miniMessage(msg.toString(),
				Placeholder.unparsed("expires", EXPIRATION_FORMAT.format(
						ZonedDateTime.ofInstant(Instant.ofEpochMilli(expires), ZoneId.systemDefault()))),
				Placeholder.unparsed("reason", reason != null ? reason.trim() : ""),
				Placeholder.unparsed("by", by != null ? by.trim() : ""));
	}
}