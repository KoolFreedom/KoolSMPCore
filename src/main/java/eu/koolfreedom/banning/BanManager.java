package eu.koolfreedom.banning;

import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.log.FLog;
import eu.koolfreedom.util.FUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class BanManager implements Listener
{
	private static final File bansFile = new File(KoolSMPCore.getInstance().getDataFolder(), "bans.yml");
	private final Map<Long, Ban> banMap = new HashMap<>();

	public BanManager()
	{
		Bukkit.getPluginManager().registerEvents(this, KoolSMPCore.getInstance());
	}

	public void load()
	{
		final File legacyPermbansFile = new File(KoolSMPCore.getInstance().getDataFolder(), "permbans.yml");
		if (legacyPermbansFile.exists())
		{
			FLog.info("Migrating legacy permanent bans file");
			final YamlConfiguration permbans = YamlConfiguration.loadConfiguration(bansFile);
			permbans.getStringList("names").forEach(name ->
			{
				long id = System.currentTimeMillis();
				banMap.put(id, Ban.builder().id(id).name(name).build());
			});
			permbans.getStringList("ips").forEach(ip ->
			{
				long id = System.currentTimeMillis();
				banMap.put(id, Ban.builder().id(id).ips(List.of(ip)).build());
			});
			legacyPermbansFile.delete();
		}

		FLog.info("Loading bans file");
		final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(bansFile);

		// Avoid wiping the ban list in memory if the configuration failed to load
		if (!banMap.isEmpty() && !configuration.getKeys(true).isEmpty())
		{
			banMap.clear();
		}

		long banId;
		for (String id : configuration.getKeys(false))
		{
			try
			{
				banId = Long.parseLong(id);
			}
			catch (NumberFormatException exception)
			{
				FLog.info("Possible legacy ban detected, assigning ID based on current time");
				banId = System.currentTimeMillis();
			}

			Ban ban = Ban.fromYamlConfiguration(banId, Objects.requireNonNull(configuration.getConfigurationSection(id)));

			if (!ban.isExpired())
			{
				banMap.put(banId, ban);
			}
		}

		FLog.info("{} bans were loaded.", banMap.size());
	}

	public void save()
	{
		FLog.info("Saving bans file");

		final YamlConfiguration storage = new YamlConfiguration();
		banMap.entrySet().stream().filter(entry -> !entry.getValue().isExpired()).forEach(entry ->
				storage.set(entry.getKey().toString(), entry.getValue().toConfigurationSection()));

		try
		{
			storage.save(bansFile);
		}
		catch (IOException ex)
		{
			FLog.error("Failed to save bans file", ex);
		}
	}

	public Optional<Ban> findBan(OfflinePlayer player)
	{
		return banMap.values().stream().filter(ban -> !ban.isExpired()).filter(ban ->
				(ban.getName() != null && player.getName() != null && player.getName().equalsIgnoreCase(ban.getName())) ||
						(ban.getUuid() != null && player.getUniqueId().equals(ban.getUuid())) ||
						(player instanceof Player actualPlayer && actualPlayer.getAddress() != null &&
								ban.getIps().contains(FUtil.getIp(actualPlayer)))).findAny();
	}

	public Optional<Ban> findBan(String value)
	{
		Objects.requireNonNull(value);

		return banMap.values().stream().filter(ban -> !ban.isExpired()).filter(ban ->
				(ban.getName() != null && value.equalsIgnoreCase(ban.getName())) ||
						(ban.getUuid() != null && value.equalsIgnoreCase(ban.getUuid().toString())) ||
						(ban.getIps().contains(value.trim()))).findAny();
	}

	public Optional<Ban> findBan(InetSocketAddress address)
	{
		return banMap.values().stream().filter(ban -> !ban.isExpired()).filter(ban ->
				ban.getIps().contains(address.getAddress().toString())).findAny();
	}

	public boolean addBan(Ban ban)
	{
		String name = ban.getName();
		UUID uuid = ban.getUuid();
		List<String> ips = ban.getIps();

		if (!ban.makesSense())
		{
			throw new IllegalArgumentException("The ban doesn't make sense");
		}

		Optional<Ban> possibleEntry = uuid != null ? findBan(uuid.toString()) :
				name != null ? findBan(name) :
						ips.stream().map(this::findBan).findAny().orElse(Optional.empty());

		// Existing entry found, we'll just overwrite it if possible
		if (possibleEntry.isPresent())
		{
			Ban oldEntry = possibleEntry.get();

			// Do not overwrite an existing permanent ban with a temporary one
			if (!oldEntry.canExpire() && ban.canExpire())
			{
				return false;
			}

			// Out with the old...
			banMap.remove(oldEntry.getId());
		}

		// In with the new!
		banMap.put(ban.getId(), ban);

		// Save the changes
		CompletableFuture.runAsync(this::save);
		return true;
	}

	public Ban removeBan(long id)
	{
		if (banMap.get(id) != null && !banMap.get(id).canExpire())
		{
			return banMap.get(id);
		}

		Ban ban = banMap.remove(id);
		CompletableFuture.runAsync(this::save);
		return ban;
	}

	public Ban removeBan(String value)
	{
		final Optional<Ban> optionalEntry = findBan(value);

		// Ban entry is present, but it can't expire as it is "permanent".
		if (optionalEntry.isPresent() && optionalEntry.filter(Ban::canExpire).isEmpty())
		{
			return optionalEntry.get();
		}
		// No ban entry was found in the first place
		else if (optionalEntry.isEmpty())
		{
			return null;
		}
		// Ban entry was found and it is temporary
		else
		{
			Ban entry = banMap.remove(optionalEntry.get().getId());
			CompletableFuture.runAsync(this::save);

			// Save the changes
			return entry;
		}
	}

	public boolean isBanned(OfflinePlayer player)
	{
		return findBan(player).isPresent();
	}

	public long getBanCount()
	{
		return banMap.values().stream().filter(ban -> !ban.isExpired()).count();
	}

	@EventHandler
	public void onPlayerJoin(PlayerLoginEvent event)
	{
		findBan(event.getPlayer()).or(() -> findBan(event.getAddress().getHostAddress())).ifPresent(ban ->
				event.disallow(PlayerLoginEvent.Result.KICK_BANNED, ban.getKickMessage()));
	}
}
