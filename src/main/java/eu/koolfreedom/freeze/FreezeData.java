package eu.koolfreedom.freeze;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import lombok.Getter;

@Getter
public class FreezeData
{
    private final Player player;
    private final Location location;

    public FreezeData(Player player)
    {
        this.player = player;
        this.location = player.getLocation();
    }
}