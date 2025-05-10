package eu.koolfreedom.punishment;

import eu.koolfreedom.banning.Ban;
import eu.koolfreedom.log.FLog;
import lombok.Builder;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.UUID;

@Builder
@Getter
public class Punishment
{
	private static final SimpleDateFormat ISSUED = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");

	// issued acts as the id as well as a fallback
	@Builder.Default
	private long issued = System.currentTimeMillis();
	@Builder.Default
	private UUID uuid = null;
	@Builder.Default
	private String name = null;
	@Builder.Default
	private String ip = null;
	@Builder.Default
	private String by = null;
	@Builder.Default
	private String reason = null;
	@Builder.Default
	private String type = null;

	public static Punishment fromBan(Ban ban)
	{
		return builder().issued(ban.getId())
				.uuid(ban.getUuid())
				.name(ban.getName())
				.ip(!ban.getIps().isEmpty() ? ban.getIps().getFirst() : null)
				.by(ban.getBy())
				.reason(ban.getReason())
				.type("BAN")
				.build();
	}

	public static Punishment fromConfigurationSection(String id, ConfigurationSection section)
	{
		PunishmentBuilder builder = builder();

		if (id != null)
		{
			builder.issued(Long.parseLong(id));
		}
		else
		{
			if (section.isLong("issued"))
			{
				builder.issued(section.getLong("issued"));
			}
			else if (section.isString("issued"))
			{
				try
				{
					builder.issued(ISSUED.parse(section.getString("issued")).getTime());
				}
				catch (ParseException e)
				{
					FLog.warning("Invalid issued timestamp");
				}
			}
		}

		if (section.isString("uuid"))
		{
			try
			{
				builder.uuid(UUID.fromString(Objects.requireNonNull(section.getString("uuid"))));
			}
			catch (IllegalArgumentException ex)
			{
				builder.uuid(null);
			}
		}

		if (section.isString("name"))
			builder.ip(section.getString("name"));

		if (section.isString("ip"))
			builder.ip(section.getString("ip"));

		if (section.isString("by"))
			builder.ip(section.getString("by"));

		if (section.isString("reason"))
			builder.ip(section.getString("reason"));

		if (section.isString("type"))
			builder.ip(section.getString("type"));

		return builder.build();
	}

	public ConfigurationSection toConfigurationSection()
	{
		MemoryConfiguration section = new MemoryConfiguration();

		section.set("issued", issued);
		if (uuid != null)
			section.set("uuid", uuid.toString());
		section.set("name", name);
		section.set("ip", ip);
		section.set("by", by);
		section.set("reason", reason);
		section.set("type", type);

		return section;
	}
}
