package eu.koolfreedom.banning;

import eu.koolfreedom.log.FLog;
import eu.koolfreedom.util.FUtil;
import lombok.Builder;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;

@Builder
@Getter
public class Ban
{
	@Getter
	private static final SimpleDateFormat expirationDateFormat = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");

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
	private List<String> ips = new ArrayList<>();

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

		builder.name(section.getString("name", null));
		builder.by(section.getString("by", section.getString("punisher", null)));
		builder.reason(section.getString("reason", null));
		builder.expires(section.getLong("expires", section.getLong("length", Long.MAX_VALUE)));

		if (section.contains("ips"))
		{
			builder.ips(new ArrayList<>(section.getStringList("ips")));
		}

		return builder.build();
	}

	public static Ban fromPlayer(OfflinePlayer player, String by, String reason, BanType type)
	{
		BanBuilder builder = builder();
		builder.name(player.getName());
		builder.uuid(player.getUniqueId());

		if (player instanceof Player onlinePlayer)
		{
			builder.ips(new ArrayList<>(List.of(FUtil.getIp(onlinePlayer))));
		}

		builder.by(by);
		builder.reason(reason);
		builder.id(System.currentTimeMillis());
		builder.expires(System.currentTimeMillis() + type.getLength());

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

	public Component getKickMessage()
	{
		StringBuilder builder = new StringBuilder("<red>You are banned from this server.");

		if (by != null)
		{
			builder.append("<newline>Banned by: <yellow><by></yellow>");
		}

		if (reason != null)
		{
			builder.append("<newline>Reason: <yellow><reason></yellow>");
		}

		if (expires != Long.MAX_VALUE)
		{
			builder.append("<newline>Expires: <yellow><expires></yellow>");
		}

		return FUtil.miniMessage(builder.toString(),
				Placeholder.unparsed("expires", expirationDateFormat.format(new Date(expires))),
				Placeholder.unparsed("reason", reason != null ? reason : ""),
				Placeholder.unparsed("by", by != null ? by : ""));
	}
}
