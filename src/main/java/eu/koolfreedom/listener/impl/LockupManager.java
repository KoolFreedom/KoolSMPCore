package eu.koolfreedom.listener.impl;

import eu.koolfreedom.listener.KoolListener;
import eu.koolfreedom.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class LockupManager extends KoolListener
{
    private final Component lockupTitle = MiniMessage.miniMessage().deserialize("<red>You are locked up!");

    public boolean toggle(Player p)
    {
        if (isLocked(p.getUniqueId()))
        {
            unlock(p.getUniqueId());
            return false;
        }
        lock(p);
        return true;
    }

    public boolean isLocked(UUID uuid)
    {
        return playerRegistry.isLockedUp(uuid);
    }

    public void lock(Player p)
    {
        Inventory fakeInv = Bukkit.createInventory(null, 27, lockupTitle);

        BukkitRunnable task = new BukkitRunnable()
        {
            @Override
            public void run()
            {
                if (!p.isOnline()) { cancel(); return; }
                var view = p.getOpenInventory();
                if (view == null || !stripColor(view.title().toString())
                        .equalsIgnoreCase(stripColor(lockupTitle.toString())))
                {
                    p.openInventory(fakeInv);
                }
            }
        };
        task.runTaskTimer(plugin, 0L, 10L);

        playerRegistry.get(p).ifPresent(data ->
        {
            if (data.getLockupTask() != null) data.getLockupTask().cancel();
            data.setLockupTask(task);
        });
        playerRegistry.setLockedUp(p.getUniqueId(), true);
        p.openInventory(fakeInv);
    }

    public void unlock(UUID uuid)
    {
        playerRegistry.get(uuid).ifPresent(data ->
        {
            if (data.getLockupTask() != null) { data.getLockupTask().cancel(); data.setLockupTask(null); }
        });
        playerRegistry.setLockedUp(uuid, false);

        Player p = Bukkit.getPlayer(uuid);
        if (p != null && p.isOnline()) p.closeInventory();
    }

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

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e)
    {
        HumanEntity clicker = e.getWhoClicked();
        if (!(clicker instanceof Player player)) return;
        if (isLocked(player.getUniqueId()) &&
                stripColor(e.getView().title().toString()).equalsIgnoreCase(stripColor(lockupTitle.toString())))
            e.setCancelled(true);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e)
    {
        HumanEntity dragger = e.getWhoClicked();
        if (!(dragger instanceof Player player)) return;
        if (isLocked(player.getUniqueId()) &&
                stripColor(e.getView().title().toString()).equalsIgnoreCase(stripColor(lockupTitle.toString())))
            e.setCancelled(true);
    }

    private String stripColor(String input)
    {
        return input.replaceAll("§[0-9a-fk-or]", "").replaceAll("[\\[\\]]", "");
    }
}
