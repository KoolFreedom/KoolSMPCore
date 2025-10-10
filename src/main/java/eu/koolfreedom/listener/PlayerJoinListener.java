package eu.koolfreedom.listener;

import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.api.AltManager;
import eu.koolfreedom.banning.BanManager;
import eu.koolfreedom.freeze.FreezeManager;
import eu.koolfreedom.note.NoteManager;
import eu.koolfreedom.note.PlayerNote;
import eu.koolfreedom.util.FLog;
import eu.koolfreedom.util.FUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PlayerJoinListener implements Listener
{
    private final KoolSMPCore plugin = KoolSMPCore.getInstance();
    private final FreezeManager freezeManager = plugin.getFreezeManager();
    private final MuteManager muteManager = plugin.getMuteManager();
    private final LockupManager lockupManager = plugin.getLockupManager();
    private final AltManager altManager = plugin.getAltManager();
    private final BanManager banManager = plugin.getBanManager();
    private final NoteManager noteManager = plugin.getNoteManager();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String ip = player.getAddress().getAddress().getHostAddress();

        // --- Ban Broadcast ---
        banManager.findBan(player)
                .or(() -> banManager.findBan(ip))
                .ifPresent(ban ->
                {
                    String duration = ban.getDurationString();
                    FUtil.broadcast("kfc.admin",
                            "<gradient:#ff4d4d:#ff9966><b>⚠ Banned Join Attempt</b></gradient> "
                                    + "<gray>-</gray> <#ffb347>" + player.getName()
                                    + "</#ffb347> <gray>tried to join but is banned</gray> "
                                    + "(<#ffd580>" + duration + "</#ffd580>)");
                    FLog.info(player.getName() + " tried to join but is banned (" + duration + ")");
                });

        // --- Staff Notes ---
        List<PlayerNote> notes = noteManager.getNotes(uuid);
        if (!notes.isEmpty())
        {
            FUtil.broadcast("kfc.admin",
                    "<gradient:#b48ef2:#e57eff>[Note]</gradient> <#d4bfff>" + player.getName()
                            + "</#d4bfff> <gray>has</gray> <#ffb3ec>" + notes.size()
                            + "</#ffb3ec> <gray>staff note(s).</gray>");

            for (PlayerNote note : notes)
            {
                FUtil.broadcast("kfc.admin",
                        "<gray>•</gray> <#c9a6ff><note></#c9a6ff>",
                        Placeholder.unparsed("note", note.getMessage()));
            }
        }

        // --- Punishment persistence ---
        if (freezeManager.isFrozen(player))
        {
            freezeManager.freeze(player);
            player.sendMessage(FUtil.miniMessage("<#CCBBF0>Just because you re-logged, doesn't mean you're safe."));
        }

        if (muteManager.isMuted(player))
        {
            player.sendMessage(FUtil.miniMessage("<#678580>You are still muted."));
        }

        if (muteManager.isCommandsBlocked(uuid))
        {
            player.sendMessage(FUtil.miniMessage("<#678580>Your commands are still blocked."));
        }

        if (lockupManager.isLocked(uuid))
        {
            lockupManager.lock(player);
            player.sendMessage(FUtil.miniMessage("<#CCBBF0>Just because you re-logged doesn't mean you're safe!"));
        }

        // --- Alt Detection ---
        altManager.record(ip, uuid);
        Set<UUID> alts = altManager.getAlts(ip);
        if (alts.size() > 1)
        {
            int altCount = alts.size() - 1;
            FUtil.broadcast("kfc.admin",
                    "<gradient:#00f5d4:#9fffac>⚠ <b>Alt Alert</b></gradient> "
                            + "<gray>-</gray> <#9fffea>" + player.getName()
                            + "</#9fffea> <gray>shares an IP with</gray> "
                            + "<#aaff80>" + altCount + "</#aaff80> <gray>other account(s).</gray>");
            FLog.info(player.getName() + " shares an IP with " + altCount + " other account(s).");
        }
    }
}
