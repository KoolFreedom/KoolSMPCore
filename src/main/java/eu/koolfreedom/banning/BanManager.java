package eu.koolfreedom.banning;

import eu.koolfreedom.listener.KoolListener;
import eu.koolfreedom.util.FLog;
import eu.koolfreedom.util.FUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerLoginEvent;

import java.net.InetSocketAddress;
import java.util.*;

public class BanManager extends KoolListener
{
	private final Map<Long, Ban> banMap = new LinkedHashMap<>();

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

	public Collection<Ban> getBans()
	{
		return Collections.unmodifiableCollection(banMap.values());
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
		Ban ban = banMap.get(id);
		if (ban != null && !ban.canExpire()) return ban; // permanent, refuse removal
		Ban removed = banMap.remove(id);
		if (removed != null) playerRegistry.deleteBan(id);
		return removed;
	}

	public Ban removeBan(String value)
	{
		Optional<Ban> opt = findBan(value);
		if (opt.isEmpty()) return null;
		Ban ban = opt.get();
		if (!ban.canExpire()) return ban; // permanent, refuse removal
		banMap.remove(ban.getId());
		playerRegistry.deleteBan(ban.getId());
		return ban;
	}

	// --- Login enforcement ---

	@EventHandler
	@SuppressWarnings("deprecation")
	public void onPlayerLogin(PlayerLoginEvent event)
	{
		findBan(event.getPlayer())
				.or(() -> findBan(event.getAddress().getHostAddress()))
				.ifPresent(ban ->
				{
					event.disallow(PlayerLoginEvent.Result.KICK_BANNED, ban.getKickMessage());
					FUtil.broadcast("kfc.admin",
							"<gradient:#ff4d4d:#ff9966><b>⚠ Banned Join Attempt</b></gradient> "
									+ "<gray>-</gray> <#ffb347>" + event.getPlayer().getName()
									+ "</#ffb347> <gray>tried to join but is banned</gray> "
									+ "(<#ffd580>" + ban.getDurationString() + "</#ffd580>)");
				});
	}
}