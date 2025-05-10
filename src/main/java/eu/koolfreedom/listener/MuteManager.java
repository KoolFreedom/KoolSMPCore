package eu.koolfreedom.listener;

import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.util.FUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MuteManager implements Listener
{
    private final List<UUID> muted = new ArrayList<>();

    public MuteManager()
    {
        Bukkit.getPluginManager().registerEvents(this, KoolSMPCore.getInstance());
    }

    public void setMuted(Player player, boolean mute)
    {
        if (mute)
        {
            muted.add(player.getUniqueId());
        }
        else
        {
            muted.remove(player.getUniqueId());
        }
    }

    public int wipeMutes()
    {
        int amount = muted.size();
        muted.clear();
        return amount;
    }

    public int getMuteCount()
    {
        return muted.size();
    }

    public boolean isMuted(Player player)
    {
        return muted.contains(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event)
    {
        Player player = event.getPlayer();

        if (isMuted(player))
        {
            player.sendMessage(FUtil.miniMessage("<gray>You are currently muted."));
            event.setCancelled(true);
        }
    }

    // Fallback in case AsyncChatEvent doesn't block it effectively enough
    @EventHandler
    @SuppressWarnings("deprecation")
    public void onPlayerChat(AsyncPlayerChatEvent event)
    {
        Player player = event.getPlayer();

        if (isMuted(player))
        {
            event.setCancelled(true);
        }
    }
}