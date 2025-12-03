package eu.koolfreedom.listener.impl;

import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.freeze.FreezeManager;
import eu.koolfreedom.listener.KoolListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;

public class FreezeListener extends KoolListener
{
    private final FreezeManager freezeManager = KoolSMPCore.getInstance().getFreezeManager();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event)
    {
        Player p  = event.getPlayer();

        if(!(freezeManager.isFrozen(p)))
        {
            return; // not frozen
        }

        if (event.getFrom().getBlockX() != event.getTo().getBlockX()
        || event.getFrom().getBlockY() != event.getTo().getBlockY()
        || event.getFrom().getBlockZ() != event.getTo().getBlockZ())
        {
            event.setCancelled(true); // hard cancel
            p.setFallDistance(0); // avoid fall-damage stuffs
            p.teleport(event.getFrom()); // rubber-band in case client desync
        }
    }
}
