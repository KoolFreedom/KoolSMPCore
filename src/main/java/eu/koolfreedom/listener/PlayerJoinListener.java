package eu.koolfreedom.listener;

import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.api.AltManager;
import eu.koolfreedom.note.NoteManager;
import eu.koolfreedom.note.PlayerNote;
import eu.koolfreedom.util.FLog;
import eu.koolfreedom.util.FUtil;
import eu.koolfreedom.freeze.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PlayerJoinListener implements Listener
{
    private final FreezeManager freezeManager = KoolSMPCore.getInstance().getFreezeManager();
    private final MuteManager muteManager = KoolSMPCore.getInstance().getMuteManager();
    private final LockupManager lockupManager = KoolSMPCore.getInstance().getLockupManager();
    private final AltManager altManager = KoolSMPCore.getInstance().getAltManager();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        NoteManager noteManager = KoolSMPCore.getInstance().getNoteManager();

        List<PlayerNote> notes = noteManager.getNotes(player.getUniqueId());

        if (notes.isEmpty())
            return;

        for (Player online : Bukkit.getOnlinePlayers())
        {
            if (!online.hasPermission("kfc.admin")) continue;

            online.sendMessage(Component.text()
                    .append(Component.text("[Note] ", NamedTextColor.YELLOW))
                    .append(Component.text(player.getName(), NamedTextColor.RED))
                    .append(Component.text(" has ", NamedTextColor.YELLOW))
                    .append(Component.text(notes.size() + " staff note(s).", NamedTextColor.RED))
            );

            for (PlayerNote note : notes)
            {
                online.sendMessage(Component.text("- " + note.getMessage(), NamedTextColor.GRAY));
            }
        }

        if (freezeManager.isFrozen(player))
        {
            freezeManager.freeze(player);
            player.sendMessage(FUtil.miniMessage("<#CCBBF0>Just because you re-logged, doesn't mean you're safe"));
        }

        if (muteManager.isMuted(player))
        {
            player.sendMessage(FUtil.miniMessage("<#678580>You are still muted."));
        }

        UUID id = player.getUniqueId();

        if (muteManager.isCommandsBlocked(id))
        {
            player.sendMessage(FUtil.miniMessage("<#678580>Your commands are still blocked."));
        }

        if (lockupManager.isLocked(player.getUniqueId()))
        {
            lockupManager.lock(player);
            player.sendMessage(FUtil.miniMessage("<#CCBBF0>Just because you re-logged doesn't mean you're safe!"));
        }

        String ip = player.getAddress().getAddress().getHostAddress();

        altManager.record(ip, player.getUniqueId());
        Set<UUID> alts = altManager.getAlts(ip);
        if (alts.size() > 1)
        {
            Component msg = Component.text("⚠ ")
                    .append(Component.text(player.getName(), NamedTextColor.RED))
                    .append(Component.text(" shares an IP with: "))
                    .append(Component.text(alts.size() - 1 + " other account(s).", NamedTextColor.YELLOW));
            Bukkit.getOnlinePlayers().stream()
                    .filter(pl -> pl.hasPermission("kfc.admin"))
                    .forEach(pl -> pl.sendMessage(msg));
            FLog.info(msg);
        }
    }

}
