package eu.koolfreedom.freeze;

import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import eu.koolfreedom.event.PlayerFreezeEvent;
import org.bukkit.scheduler.BukkitTask;

public class FreezeManager
{
    private final Map<UUID, FreezeData> frozenPlayers = new ConcurrentHashMap<>();
    @Getter
    private BukkitTask unfreezeTask;

    public void freeze(Player player)
    {
        unfreeze(player);

        FreezeData data = new FreezeData(player);

        PlayerFreezeEvent event = new PlayerFreezeEvent(player, PlayerFreezeEvent.FreezeState.FROZEN, data);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled())
            return;

        frozenPlayers.put(player.getUniqueId(), data);
    }

    public void unfreeze(UUID uuid)
    {
        FreezeData data = frozenPlayers.remove(uuid);
        if (data == null)
            return; // wasn't frozen, don't fire a spurious event

        Player p = Bukkit.getPlayer(uuid);
        if (p != null && p.isOnline())
        {
            p.closeInventory();
            Bukkit.getPluginManager().callEvent(
                    new PlayerFreezeEvent(p, PlayerFreezeEvent.FreezeState.UNFROZEN, data)
            );
        }
    }

    public void unfreeze(Player player)
    {
        if (player != null)
            unfreeze(player.getUniqueId());
    }

    public boolean isFrozen(Player player)
    {
        return frozenPlayers.containsKey(player.getUniqueId());
    }

    public FreezeData getData(Player player)
    {
        return frozenPlayers.get(player.getUniqueId());
    }

    public boolean isFrozen()
    {
        return unfreezeTask != null;
    }

    public void clearTask()
    {
        if (unfreezeTask != null) unfreezeTask.cancel();
        unfreezeTask = null;
    }

    public void unfreezeAll()
    {
        for (UUID id : new HashSet<>(frozenPlayers.keySet()))
        {
            unfreeze(id);
        }
    }
}