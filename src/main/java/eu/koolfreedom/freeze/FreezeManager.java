package eu.koolfreedom.freeze;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FreezeManager
{
    private final Map<UUID, FreezeData> frozenPlayers = new ConcurrentHashMap<>();

    public void freeze(Player player)
    {
        unfreeze(player);                                   // clear any previous entry
        frozenPlayers.put(player.getUniqueId(), new FreezeData(player)); // <-- pass Player
    }

    public void unfreeze(UUID uuid)
    {
        FreezeData data = frozenPlayers.remove(uuid);
        if (data != null) data.clearTask();          // cancel the timer task

        // Optional: close their inventory / notify, if they're online
        Player p = Bukkit.getPlayer(uuid);
        if (p != null && p.isOnline())
            p.closeInventory();
    }

    /** Convenience wrapper for existing code that passes a Player */
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

    /** Unfreeze everyone currently frozen */
    public void unfreezeAll()
    {
        // Avoid ConcurrentModificationException by copying the keys first
        for (UUID id : new HashSet<>(frozenPlayers.keySet()))
            unfreeze(id);
    }
}
