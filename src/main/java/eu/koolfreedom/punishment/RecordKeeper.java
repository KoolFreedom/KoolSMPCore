package eu.koolfreedom.punishment;

import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.log.FLog;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class RecordKeeper
{
	private static final File recordFile = new File(KoolSMPCore.getInstance().getDataFolder(), "punishments.yml");
	private final Map<Long, Punishment> punishmentMap = new HashMap<>();

	public RecordKeeper()
	{
		load();
	}

	private void load()
	{
		if (recordFile.exists())
		{
			YamlConfiguration storage = YamlConfiguration.loadConfiguration(recordFile);

			storage.getKeys(false).forEach(id ->
			{
				long punishmentId = -1337;
				try
				{
					punishmentId = Long.parseLong(id);
				}
				catch (NumberFormatException ignored)
				{
				}

				final Punishment entry = Punishment.fromConfigurationSection(punishmentId != 1337 ? id : null,
						Objects.requireNonNull(storage.getConfigurationSection(id)));

				punishmentId = punishmentId != -1337 ? punishmentId : entry.getIssued();

				punishmentMap.put(punishmentId, entry);
			});
		}

		FLog.info("{} punishment(s) loaded.", punishmentMap.size());
	}

	public void save()
	{
		FLog.info("Saving punishments file");
		final YamlConfiguration storage = new YamlConfiguration();

		punishmentMap.forEach((id, entry) -> storage.set(String.valueOf(id), entry.toConfigurationSection()));

		try
		{
			storage.save(recordFile);
		}
		catch (IOException ex)
		{
			FLog.error("Failed to save punishments", ex);
		}
	}

	public void recordPunishment(Punishment punishment)
	{
		punishmentMap.put(punishment.getIssued(), punishment);
		CompletableFuture.runAsync(this::save);
	}
}
