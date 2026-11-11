package eu.koolfreedom.listener.impl;

import eu.koolfreedom.command.impl.MuteChatCommand;
import eu.koolfreedom.listener.KoolListener;
import eu.koolfreedom.util.FUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MuteManager extends KoolListener
{
    private final Map<UUID, BukkitTask> muteTasks = new ConcurrentHashMap<>();

    // --- Public API ---

    public void mute(UUID uuid)
    {
        cancelMuteTask(uuid);
        playerRegistry.setMuted(uuid, true);
    }

    public void mute(UUID uuid, long durationTicks)
    {
        mute(uuid);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isMuted(uuid))
            {
                unmute(uuid);
                Player online = Bukkit.getPlayer(uuid);
                if (online != null)
                {
                    online.sendMessage(FUtil.miniMessage("<green>Your mute has expired."));
                }
            }
            else
            {
                muteTasks.remove(uuid);
            }
        }, Math.max(1L, durationTicks));
        muteTasks.put(uuid, task);
    }

    public void unmute(UUID uuid)
    {
        cancelMuteTask(uuid);
        playerRegistry.setMuted(uuid, false);
    }

    public boolean isMuted(UUID uuid)
    {
        return playerRegistry.isMuted(uuid);
    }

    public void setCommandsBlocked(UUID uuid, boolean block)
    {
        playerRegistry.setCommandsBlocked(uuid, block);
    }

    public boolean isCommandsBlocked(UUID uuid)
    {
        return playerRegistry.isCommandsBlocked(uuid);
    }

    public int wipeMutes()
    {
        muteTasks.values().forEach(BukkitTask::cancel);
        muteTasks.clear();
        return playerRegistry.clearMutes();
    }

    private void cancelMuteTask(UUID uuid)
    {
        BukkitTask task = muteTasks.remove(uuid);
        if (task != null)
        {
            task.cancel();
        }
    }

    public void wipeBlockedCommands()
    {
        playerRegistry.clearCommandsBlocked();
    }

    // Convenience overloads
    public void mute(Player p)
    {
        mute(p.getUniqueId());
    }
    public void unmute(Player p)
    {
        unmute(p.getUniqueId());
    }

    public boolean isMuted(Player p)
    {
        return isMuted(p.getUniqueId());
    }

    public boolean isMuted(OfflinePlayer p)
    {
        return isMuted(p.getUniqueId());
    }

    // --- Event listeners ---

    @EventHandler
    public void onChat(AsyncChatEvent e)
    {
        if (isMuted(e.getPlayer()))
        {
            e.getPlayer().sendMessage(FUtil.miniMessage("<gray>You are muted."));
            e.setCancelled(true);
        }

        if (MuteChatCommand.isChatMuted() && !e.getPlayer().hasPermission("kfc.mutechat.bypass"))
        {
            e.setCancelled(true);
            e.getPlayer().sendMessage(FUtil.miniMessage("<red>Chat is currently muted, you cannot speak."));
        }
    }

    @EventHandler
    @SuppressWarnings("deprecation")
    public void onLegacyChat(AsyncPlayerChatEvent e)
    {
        if (isMuted(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler
    public void onCmd(PlayerCommandPreprocessEvent e)
    {
        UUID id = e.getPlayer().getUniqueId();

        if (isCommandsBlocked(id))
        {
            e.setCancelled(true);
            e.getPlayer().sendMessage(FUtil.miniMessage("<red>Your commands are blocked."));
            return;
        }

        if (isMuted(id))
        {
            e.setCancelled(true);
            e.getPlayer().sendMessage(FUtil.miniMessage("<red>You are muted, you cannot use commands."));
        }

        if (MuteChatCommand.isChatMuted() && !e.getPlayer().hasPermission("kfc.mutechat.bypass"))
        {
            e.setCancelled(true);
            e.getPlayer().sendMessage(FUtil.miniMessage("<red>Chat is currently muted, you cannot speak."));
        }
    }
}
