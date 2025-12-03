package eu.koolfreedom.freeze;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class FreezeData {
    @Getter
    private final Player player;
    private final Location location;
    @Setter
    private BukkitTask unfreezeTask;

    public FreezeData(Player player)
    {
        this.player = player;
        this.location = player.getLocation();
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

    public Location getFrozenLocation()
    {
        return location;
    }
}
