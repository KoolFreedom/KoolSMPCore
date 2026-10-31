package eu.koolfreedom.banning;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import eu.koolfreedom.listener.KoolListener;
import eu.koolfreedom.util.FLog;
import eu.koolfreedom.util.FUtil;

public class BanManager extends KoolListener
{
	private final Map<Long, Ban> banMap = new ConcurrentHashMap<>();

	public void load()
	{
		banMap.clear();
		banMap.putAll(playerRegistry.loadAllBans());
		FLog.info("{} ban(s) loaded.", banMap.size());
	}

	// --- Lookups ---

	public Optional<Ban> findBan(OfflinePlayer player)
	{
		return banMap.values().stream()
				.filter(b -> !b.isExpired())
				.filter(b ->
						(b.getName() != null && player.getName() != null && player.getName().equalsIgnoreCase(b.getName())) ||
								(b.getUuid() != null && player.getUniqueId().equals(b.getUuid())) ||
								(player instanceof Player online && online.getAddress() != null &&
										b.getIps().contains(FUtil.getIp(online))))
				.findAny();
	}

	public Optional<Ban> findBan(String value)
	{
		Objects.requireNonNull(value);
		return banMap.values().stream()
				.filter(b -> !b.isExpired())
				.filter(b ->
						(b.getName() != null && value.equalsIgnoreCase(b.getName())) ||
								(b.getUuid() != null && value.equalsIgnoreCase(b.getUuid().toString())) ||
								b.getIps().contains(value.trim()))
				.findAny();
	}

	public Optional<Ban> findBan(InetSocketAddress address)
	{
		return banMap.values().stream()
				.filter(b -> !b.isExpired())
				.filter(b -> b.getIps().contains(address.getAddress().getHostAddress()))
				.findAny();
	}

	public boolean isBanned(OfflinePlayer player)
	{
		return findBan(player).isPresent();
	}

	public long getBanCount()
	{
		return banMap.values().stream().filter(b -> !b.isExpired()).count();
	}

	public List<Ban> getActiveBans()
	{
		return banMap.values().stream()
				.filter(ban -> !ban.isExpired())
				.sorted(Comparator.comparingLong(Ban::getId).reversed())
				.toList();
	}

	/**
	 * Adds a ban. Returns false if a permanent ban already exists and the new ban is temporary.
	 */
	public boolean addBan(Ban ban)
	{
		if (!ban.makesSense()) throw new IllegalArgumentException("Ban doesn't make sense");

		Optional<Ban> existing = ban.getUuid() != null ? findBan(ban.getUuid().toString())
				: ban.getName() != null ? findBan(ban.getName())
				  : ban.getIps().stream().map(this::findBan).filter(Optional::isPresent).map(Optional::get).findFirst().map(Optional::of).orElse(Optional.empty());

		if (existing.isPresent())
		{
			Ban old = existing.get();
			if (!old.canExpire() && ban.canExpire()) return false;
			banMap.remove(old.getId());
			playerRegistry.deleteBan(old.getId());
		}

		banMap.put(ban.getId(), ban);
		playerRegistry.saveBan(ban);
		return true;
	}

	public Ban removeBan(long id)
	{
		return removeBan(id, false);
	}

	public Ban removeBan(long id, boolean allowPermanent)
	{
		Ban ban = banMap.get(id);
		if (ban != null && !ban.canExpire() && !allowPermanent) return null;
		Ban removed = banMap.remove(id);
		if (removed != null) playerRegistry.deleteBan(id);
		return removed;
	}

	public Ban removeBan(String value)
	{
		return removeBan(value, false);
	}

	public Ban removeBan(String value, boolean allowPermanent)
	{
		Optional<Ban> opt = findBan(value);
		if (opt.isEmpty()) return null;
		Ban ban = opt.get();
		return removeBan(ban.getId(), allowPermanent);
	}

	// --- Login enforcement ---

	@EventHandler
	public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event)
	{
		if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED)
		{
			return;
		}

		Optional<Ban> ban = findBan(event.getUniqueId().toString());
		if (ban.isEmpty())
		{
			ban = findBan(event.getName());
		}
		if (ban.isEmpty())
		{
			ban = findBan(event.getAddress().getHostAddress());
		}

		if (ban.isPresent())
		{
			Ban activeBan = ban.get();
			event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, activeBan.getKickMessage());
			plugin.getServer().getScheduler().runTask(plugin,
					() -> announceBannedJoin(event.getName(), activeBan));
		}
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		Player player = event.getPlayer();
		findBan(player).ifPresent(ban ->
		{
			player.kick(ban.getKickMessage());
		});
	}

	private void announceBannedJoin(String playerName, Ban ban)
	{
		FUtil.broadcast("kfc.admin",
				"<gradient:#ff4d4d:#ff9966><b>⚠ Banned Join Attempt</b></gradient> "
						+ "<gray>-</gray> <#ffb347>" + (playerName != null ? playerName : "Unknown")
						+ "</#ffb347> <gray>tried to join but is banned</gray> "
						+ "(<#ffd580>" + ban.getDurationString() + "</#ffd580>)");
	}
}