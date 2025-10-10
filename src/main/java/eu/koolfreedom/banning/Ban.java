package eu.koolfreedom.banning;

import eu.koolfreedom.config.ConfigEntry;
import eu.koolfreedom.util.FLog;
import eu.koolfreedom.util.FUtil;
import lombok.Builder;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
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
	@Builder.Default
	private UUID uuid = null;
	@Builder.Default
	private String name = null;
	@Builder.Default
	private String by = null;
	@Builder.Default
	private String reason = null;
	@Builder.Default
	private long expires = Long.MAX_VALUE;
	@Builder.Default
	private List<String> ips = Collections.emptyList();

	public static Ban fromYamlConfiguration(Long id, ConfigurationSection section)
	{
		BanBuilder builder = builder();

		builder.id(section.getLong("id", id));

		if (section.contains("uuid"))
		{
			try
			{
				builder.uuid(UUID.fromString(Objects.requireNonNull(section.getString("uuid"))));
			}
			catch (IllegalArgumentException ex)
			{
				FLog.warning("Entry {} contains invalid UUID: {}", builder.id, section.get("uuid"));
			}
		}

		String name = section.getString("name");
		if (name != null && !name.isBlank()) builder.name(name);

		String punisher = section.getString("by", section.getString("punisher", null));
		if (punisher != null && !punisher.isBlank()) builder.by(punisher);

		String reason = section.getString("reason");
		if (reason != null && !reason.isBlank()) builder.reason(reason);

		builder.expires(section.getLong("expires", section.getLong("length", Long.MAX_VALUE)));

		if (section.contains("ips"))
		{
			builder.ips(new ArrayList<>(section.getStringList("ips")));
		}

		return builder.build();
	}

	public static Ban fromPlayer(OfflinePlayer player, String by, String reason, long duration)
	{
		BanBuilder builder = builder();
		builder.name(player.getName());
		builder.uuid(player.getUniqueId());

		if (player instanceof Player onlinePlayer)
		{
			builder.ips(new ArrayList<>(List.of(FUtil.getIp(onlinePlayer))));
		}

		long now = System.currentTimeMillis();
		builder.id(now);
		builder.by(by);
		builder.reason(reason);
		builder.expires(duration != Long.MAX_VALUE ? now + duration : Long.MAX_VALUE);

		return builder.build();
	}

	public ConfigurationSection toConfigurationSection()
	{
		final MemoryConfiguration section = new MemoryConfiguration();

		section.set("id", id);
		if (uuid != null) section.set("uuid", uuid.toString());
		section.set("name", name);
		section.set("by", by);
		section.set("reason", reason);
		section.set("expires", expires);
		section.set("ips", ips);

		return section;
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
        if (!canExpire())
        {
            return "permanent";
        }

        long remaining = expires - System.currentTimeMillis();
        if (remaining <= 0)
        {
            return "expired";
        }

        long seconds = remaining / 1000;
        long days = seconds / 86400;
        seconds %= 86400;
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m");

        return sb.isEmpty() ? "less than a minute" : sb.toString().trim();
    }


    public Component getKickMessage()
	{
		StringBuilder builder = new StringBuilder("<red>You are banned from this server.");

		if (by != null && !by.isBlank())
		{
			builder.append("<newline>Banned by: <yellow><by></yellow>");
		}

		if (reason != null && !reason.isBlank())
		{
			builder.append("<newline>Reason: <yellow><reason></yellow>");
		}

		if (expires != Long.MAX_VALUE)
		{
			builder.append("<newline>Expires: <yellow><expires></yellow>");
		}

		builder.append("<newline><red>You can appeal at <yellow>")
				.append(ConfigEntry.SERVER_APPEAL_URL.getString())
				.append("</yellow>");

		return FUtil.miniMessage(builder.toString(),
				Placeholder.unparsed("expires", EXPIRATION_FORMAT.format(ZonedDateTime.ofInstant(
						Instant.ofEpochMilli(expires), ZoneId.systemDefault()))),
				Placeholder.unparsed("reason", reason != null ? reason.trim() : ""),
				Placeholder.unparsed("by", by != null ? by.trim() : ""));
	}
}
