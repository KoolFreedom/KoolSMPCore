package eu.koolfreedom.freeze;

import eu.koolfreedom.player.PlayerData;
import eu.koolfreedom.player.PlayerRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class FreezeManager
{
    private final PlayerRegistry playerRegistry;

    public FreezeManager(PlayerRegistry playerRegistry)
    {
        this.playerRegistry = playerRegistry;
    }

    public void freeze(Player player)
    {
        playerRegistry.get(player).ifPresent(data ->
        {
            if (data.getFreezeData() != null) data.getFreezeData().clearTask();
            data.setFreezeData(new FreezeData(player));
        });
        playerRegistry.setFrozen(player.getUniqueId(), true);
    }

    public void unfreeze(UUID uuid)
    {
        playerRegistry.get(uuid).ifPresent(data ->
        {
            if (data.getFreezeData() != null)
            {
                data.getFreezeData().clearTask();
                data.setFreezeData(null);
            }
        });
        playerRegistry.setFrozen(uuid, false);

        Player p = Bukkit.getPlayer(uuid);
        if (p != null && p.isOnline()) p.closeInventory();
    }

    public void unfreeze(Player player)
    {
        if (player != null) unfreeze(player.getUniqueId());
    }

    public boolean isFrozen(Player player)
    {
        return playerRegistry.isFrozen(player.getUniqueId());
    }

    public FreezeData getData(Player player)
    {
        return playerRegistry.get(player).map(PlayerData::getFreezeData).orElse(null);
    }

    public void unfreezeAll()
    {
        for (PlayerData data : playerRegistry.getAll())
        {
            if (data.isFrozen()) unfreeze(data.getUuid());
        }
    }
}
