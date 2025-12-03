package eu.koolfreedom.util;

import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.freeze.FreezeManager;
import eu.koolfreedom.listener.impl.MuteManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AutoUndoManager
{
    private final KoolSMPCore plugin;
    private final MuteManager muteManager;
    private final FreezeManager freezeManager;

    private final Map<UUID, BukkitTask> muteTasks = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> freezeTasks = new ConcurrentHashMap<>();

    private static final long AUTO_UNDO_TICKS = 5 * 60 * 20L;

    private static final Component AUTO_UNMUTE_MSG   = FUtil.miniMessage("<green>Your mute has been automatically lifted after 5 minutes.");
    private static final Component AUTO_UNFREEZE_MSG = FUtil.miniMessage("<green>Your freeze has been automatically lifted after 5 minutes.");

    public AutoUndoManager(KoolSMPCore plugin, MuteManager muteManager, FreezeManager freezeManager)
    {
        this.plugin = plugin;
        this.muteManager = muteManager;
        this.freezeManager = freezeManager;
    }

    /* ------------------------- */
    /* Mute auto-undo */
    /* ------------------------- */

    public void scheduleAutoUnmute(Player player)
    {
        UUID uuid = player.getUniqueId();
        scheduleUndo(uuid, muteTasks, () -> {
            if (!muteManager.isMuted(uuid)) return;

            muteManager.unmute(uuid);
            Player online = Bukkit.getPlayer(uuid);
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);

            String name = online != null ? online.getName() : Objects.requireNonNull(offline.getName());
            FUtil.staffAction(Bukkit.getConsoleSender(), "Auto-unmuted <player>",
                    Placeholder.unparsed("player", name));

            if (online != null) online.sendMessage(AUTO_UNMUTE_MSG);
        });
    }

    public void cancelAutoUnmute(UUID uuid)
    {
        cancelTask(uuid, muteTasks);
    }

    /* ------------------------- */
    /* Freeze auto-undo */
    /* ------------------------- */

    public void scheduleAutoUnfreeze(Player player)
    {
        UUID uuid = player.getUniqueId();
        scheduleUndo(uuid, freezeTasks, () -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) return;

            if (!freezeManager.isFrozen(p)) return;

            FUtil.staffAction(Bukkit.getConsoleSender(), "Auto-unfroze <player>",
                    Placeholder.unparsed("player", p.getName()));
            p.sendMessage(AUTO_UNFREEZE_MSG);
            freezeManager.unfreeze(p);
        });
    }

    public void cancelAutoUnfreeze(UUID uuid)
    {
        cancelTask(uuid, freezeTasks);
    }

    /* ------------------------- */
    /* Generic helpers */
    /* ------------------------- */

    private void scheduleUndo(UUID uuid, Map<UUID, BukkitTask> map, Runnable undoAction)
    {
        // Cancel existing
        cancelTask(uuid, map);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    undoAction.run();
                } finally {
                    map.remove(uuid);
                }
            }
        }.runTaskLater(plugin, AUTO_UNDO_TICKS);

        map.put(uuid, task);
    }

    private void cancelTask(UUID uuid, Map<UUID, BukkitTask> map)
    {
        BukkitTask task = map.remove(uuid);
        if (task != null) task.cancel();
    }
}
