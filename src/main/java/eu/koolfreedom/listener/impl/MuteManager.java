package eu.koolfreedom.listener.impl;

import eu.koolfreedom.command.impl.MuteChatCommand;
import eu.koolfreedom.listener.KoolListener;
import eu.koolfreedom.util.FUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.UUID;

public class MuteManager extends KoolListener
{
    // --- Public API ---

    public void mute(UUID uuid)
    {
        playerRegistry.setMuted(uuid, true);
    }

    public void unmute(UUID uuid)
    {
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
        return playerRegistry.clearMutes();
    }

    public int wipeBlockedCommands()
    {
        return playerRegistry.clearCommandsBlocked();
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
