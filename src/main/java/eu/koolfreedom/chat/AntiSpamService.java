package eu.koolfreedom.chat;

import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.listener.MuteManager;
import eu.koolfreedom.util.FUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AntiSpamService implements Listener
{
    private static final int CHAT_MAX_PER_SEC      = 5;   // msgs-per-second before mute
    private static final int CMD_MAX_PER_SEC       = 5;   // cmds-per-second before kick
    private static final int WARN_AT_CHAT_MESSAGES = CHAT_MAX_PER_SEC / 2;
    private static final String BYPASS_PERMISSION  = "kfc.antispam.bypass";

    private static final Component CHAT_WARN_MSG =
            FUtil.miniMessage("<gray>Please slow down | spamming is not allowed.");
    private static final Component CMD_KICK_MSG =
            FUtil.miniMessage("<red>You were kicked for spamming commands.");

    private final Map<UUID, Counter> chatCounts = new ConcurrentHashMap<>();
    private final Map<UUID, Counter> cmdCounts  = new ConcurrentHashMap<>();

    private final MuteManager muteManager;
    private final KoolSMPCore plugin;

    public AntiSpamService(KoolSMPCore plugin)
    {
        this.plugin = plugin;
        this.muteManager = plugin.getMuteManager();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /* ----------------------- CHAT ----------------------- */

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e)
    {
        Player p = e.getPlayer();
        if (p.hasPermission(BYPASS_PERMISSION)) return;
        if (muteManager.isMuted(p)) return;   // already muted

        int count = incrementAndGet(p.getUniqueId(), chatCounts);

        if (count > CHAT_MAX_PER_SEC)
        {
            muteManager.mute(p);                                   // permanent mute
            plugin.getAutoUndoManager().scheduleAutoUnmute(p);     // auto-undo in 5min
            FUtil.staffAction(Bukkit.getConsoleSender(),
                    "Auto-muted <player> for spamming chat",
                    Placeholder.unparsed("player", p.getName()));
            e.setCancelled(true);
        }
        else if (count >= WARN_AT_CHAT_MESSAGES)
        {
            p.sendMessage(CHAT_WARN_MSG);
            e.setCancelled(true);
        }
    }

    /* -------------------- COMMANDS --------------------- */

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e)
    {
        Player p = e.getPlayer();
        if (p.hasPermission(BYPASS_PERMISSION)) return;

        int count = incrementAndGet(p.getUniqueId(), cmdCounts);

        if (count > CMD_MAX_PER_SEC)
        {
            FUtil.staffAction(Bukkit.getConsoleSender(),
                    "Auto-kicked <player> for spamming commands",
                    Placeholder.unparsed("player", p.getName()));
            p.kick(CMD_KICK_MSG);
            e.setCancelled(true);
        }
    }

    /* --------------------- Helpers --------------------- */

    private int incrementAndGet(UUID uuid, Map<UUID, Counter> map)
    {
        int now = (int)(System.currentTimeMillis() / 1000);
        Counter c = map.computeIfAbsent(uuid, k -> new Counter());
        if (c.second != now)
        {
            c.second = now;
            c.count = 0;
        }
        return ++c.count;
    }

    private static class Counter
    {
        int second;
        int count;
    }
}
