package eu.koolfreedom.listener.impl;

import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.listener.KoolListener;
import eu.koolfreedom.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LockupManager extends KoolListener
{
    private final Map<UUID, BukkitRunnable> locked = new ConcurrentHashMap<>();
    private final MiniMessage mini = MiniMessage.miniMessage();
    private final Component lockupTitle = mini.deserialize("<red>You are locked up!");

    /* --------------------------------------------------------------------- */
    /*  Public API                                                           */
    /* --------------------------------------------------------------------- */

    /** @return the new state (true = now locked, false = now unlocked) */
    public boolean toggle(Player p)
    {
        if (locked.containsKey(p.getUniqueId()))
        {
            unlock(p.getUniqueId());
            return false;
        }
        else
        {
            lock(p);
            return true;
        }
    }

    public boolean isLocked(UUID uuid)
    {
        return locked.containsKey(uuid);
    }

    /* --------------------------------------------------------------------- */
    /*  Implementation                                                       */
    /* --------------------------------------------------------------------- */

    public void lock(Player p)
    {
        Inventory fakeInv = Bukkit.createInventory(null, 27, lockupTitle);

        BukkitRunnable task = new BukkitRunnable()
        {
            @Override
            public void run()
            {
                if (!p.isOnline())
                {
                    cancel();
                    return;
                }

                InventoryView view = p.getOpenInventory();
                if (view == null || !Component.text(stripColor(view.title().toString())).equals(stripColor(lockupTitle.toString())))
                {
                    p.openInventory(fakeInv);
                }
            }
        };
        task.runTaskTimer(plugin, 0L, 10L); // every 0.5s
        locked.put(p.getUniqueId(), task);
        p.openInventory(fakeInv);
    }

    public void unlock(UUID uuid)
    {
        BukkitRunnable task = locked.remove(uuid);
        if (task != null) task.cancel();
        Player p = Bukkit.getPlayer(uuid);
        if (p != null && p.isOnline())
            p.closeInventory();
    }

    /* --------------------------------------------------------------------- */
    /*  Event guards – stop locked players from moving or running commands   */
    /* --------------------------------------------------------------------- */

    @EventHandler
    public void onMove(PlayerMoveEvent e)
    {
        if (isLocked(e.getPlayer().getUniqueId()) && e.getFrom().getBlockX() != e.getTo().getBlockX())
            e.setTo(e.getFrom());
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e)
    {
        if (isLocked(e.getPlayer().getUniqueId()))
        {
            e.setCancelled(true);
            e.getPlayer().sendMessage(FUtil.miniMessage("<red>You are locked‑up and cannot use commands."));
        }
    }

    @EventHandler
    @SuppressWarnings("deprecation")
    public void onPlayerChat(AsyncPlayerChatEvent e)
    {
        if (isLocked(e.getPlayer().getUniqueId()))
        {
            e.setCancelled(true);
            e.getPlayer().sendMessage(FUtil.miniMessage("<red>You are locked-up and cannot chat."));
        }
    }

    /* --------------------------------------------------------------------- */
    /*  GUI Interaction Blocking (click, drag, etc.)                         */
    /* --------------------------------------------------------------------- */

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e)
    {
        HumanEntity clicker = e.getWhoClicked();
        if (!(clicker instanceof Player player)) return;

        if (isLocked(player.getUniqueId()) &&
                stripColor(e.getView().title().toString()).equalsIgnoreCase(stripColor(lockupTitle.toString())))
        {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e)
    {
        HumanEntity dragger = e.getWhoClicked();
        if (!(dragger instanceof Player player)) return;

        if (isLocked(player.getUniqueId()) &&
                stripColor(e.getView().title().toString()).equalsIgnoreCase(stripColor(lockupTitle.toString())))
        {
            e.setCancelled(true);
        }
    }

    private String stripColor(String input)
    {
        return input.replaceAll("§[0-9a-fk-or]", "").replaceAll("[\\[\\]]", "");
    }
}
