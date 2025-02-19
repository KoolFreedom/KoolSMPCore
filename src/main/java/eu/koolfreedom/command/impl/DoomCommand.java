package eu.koolfreedom.command.impl;

import eu.koolfreedom.KoolSMPCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public class DoomCommand implements CommandExecutor {
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /" + s + " <player> [reason]", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Could not find the specified player on the server.", NamedTextColor.RED));
            return true;
        }
        if (!sender.hasPermission("kf.senior")) {
            sender.sendMessage(NamedTextColor.RED + "You do not have permission to use this command");
            return false;
        }

        KoolSMPCore.adminAction(sender.getName(), "Swinging the Russian Hammer over " + target.getName(), true);
        KoolSMPCore.bcastMsg(target.getName() + " will be squished flat", ChatColor.RED);

        KoolSMPCore.adminAction(sender.getName(), "Removing " + target.getName() + " from the staff list", true);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + target.getName() + " clear");

        // Remove from whitelist
        target.setWhitelisted(false);

        // De-op
        target.setOp(false);

        String reason = args.length > 1 ? " (" + String.join(" ", Arrays.copyOfRange(args, 1, args.length)) + ")" : "";

        // Set gamemode to survival
        target.setGameMode(GameMode.SURVIVAL);

        // Ignite player
        target.setFireTicks(10000);

        // Explosions
        target.getWorld().createExplosion(target.getLocation(), 0F, false);

        new BukkitRunnable() {
            @Override
            public void run()
            {
                // strike lightning
                target.getWorld().strikeLightningEffect(target.getLocation());

                // kill if not killed already
                target.setHealth(0.0);
            }
        }.runTaskLater(KoolSMPCore.main, 2L * 20L);

        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                // message
                KoolSMPCore.adminAction(sender.getName(), "Banning " + target.getName(), true);

                // more explosion
                target.getWorld().createExplosion(target.getLocation(), 0F, false);

                // execute ban command
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "banip " + target.getName() + " &c&lMay your worst nightmare come true, and may you suffer by the hands of your ruler. " + reason + " -s");
            }
        }.runTaskLater(KoolSMPCore.main, 3L * 20L);
        return true;
    }
}
