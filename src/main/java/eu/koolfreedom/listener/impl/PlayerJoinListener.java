package eu.koolfreedom.listener.impl;

import eu.koolfreedom.listener.KoolListener;
import eu.koolfreedom.player.PlayerData;
import eu.koolfreedom.util.FLog;
import eu.koolfreedom.util.FUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;
import java.util.UUID;

public class PlayerJoinListener extends KoolListener
{
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();

        PlayerData data = playerRegistry.require(player);

        // --- Staff Notes ---
        if (data.hasNotes())
        {
            FUtil.broadcast("kfc.admin",
                    "<gradient:#b48ef2:#e57eff>[Note]</gradient> <#d4bfff>"
                            + player.getName() + "</#d4bfff> <gray>has</gray> <#ffb3ec>"
                            + data.getNotes().size() + "</#ffb3ec> <gray>staff note(s).</gray>");

            data.getNotes().forEach(note ->
                    FUtil.broadcast("kfc.admin",
                            "<gray>•</gray> <#c9a6ff><note></#c9a6ff>",
                            Placeholder.unparsed("note", note.message())));
        }

        // --- Restore persistent moderation state ---
        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                if (!player.isOnline()) return;

                if (data.isFrozen())
                {
                    freezeManager.freeze(player);
                    player.sendMessage(FUtil.miniMessage(
                            "<#CCBBF0>Just because you re-logged, doesn't mean you're safe."));
                }

                if (data.isMuted())
                    player.sendMessage(FUtil.miniMessage("<#678580>You are still muted."));

                if (data.isCommandsBlocked())
                    player.sendMessage(FUtil.miniMessage("<#678580>Your commands are still blocked."));

                if (data.isLockedUp())
                {
                    lockupManager.lock(player);
                    player.sendMessage(FUtil.miniMessage(
                            "<#CCBBF0>Just because you re-logged doesn't mean you're safe!"));
                }
            }
        }.runTaskLater(plugin, 40L);

        // --- Alt detection ---
        String ip = FUtil.getIp(player);
        if (ip != null)
        {
            Set<UUID> alts = playerRegistry.getAlts(ip);
            if (alts.size() > 1)
            {
                int altCount = alts.size() - 1;
                FUtil.broadcast("kfc.admin",
                        "<gradient:#00f5d4:#9fffac>⚠ <b>Alt Alert</b></gradient> "
                                + "<gray>-</gray> <#9fffea>" + player.getName()
                                + "</#9fffea> <gray>shares an IP with</gray> "
                                + "<#aaff80>" + altCount + "</#aaff80> <gray>other account(s).</gray>");
                FLog.info("[Alt Alert] " + player.getName() + " shares an IP with " + altCount + " other account(s).");
            }
        }
    }
}