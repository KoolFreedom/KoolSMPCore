package eu.koolfreedom.command.impl;

import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.command.FreedomCommand;
import eu.koolfreedom.command.impl.Messages;
import eu.koolfreedom.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;


public class ObliterateCommand extends FreedomCommand {
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /" + commandLabel + " <player> [reason]", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Messages.PLAYER_NOT_FOUND);
            return true;
        }

        if (!sender.hasPermission("kf.senior")) {
            sender.sendMessage(Messages.MSG_NO_PERMS);
            return false;
        }

        FUtil.adminAction(sender.getName(), "Unleashing Majora's Wrath upon " + target.getName(), true);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "discord bcast **" + sender.getName() + " - Unleashing Majora's Wrath upon " + target.getName() + "**");
        FUtil.bcastMsg(target.getName() + " will never see the light of day", ChatColor.RED);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "discord bcast **" + target.getName() + " will never see the light of day**");

        FUtil.adminAction(sender.getName(), "Removing " + target.getName() + " from the staff list", true);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "discord bcast **Removing " + target.getName() + " from the staff list**");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + target.getName() + " clear");

        // Remove from whitelist
        target.setWhitelisted(false);

        // De-op
        target.setOp(false);


        // Set gamemode to survival
        target.setGameMode(GameMode.SURVIVAL);

        // Ignite player
        target.setFireTicks(10000);

        // Explosions
        target.getWorld().createExplosion(target.getLocation(), 0F, false);

        // crash
        for (int i = 0; i < 3; i++) {
            Bukkit.getScheduler().runTaskLater(KoolSMPCore.main, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute as \"" + target.getName() + "\" at @s run particle flame ~ ~ ~ 1 1 1 1 999999999 force @s"), 30);
        }

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
                // discord message
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "discord bcast **" + target.getName() + " has met with a terrible fate**");

                // more explosion
                target.getWorld().createExplosion(target.getLocation(), 0F, false);

                // add ban
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "banip " + target.getName() + " You've met with a terrible fate haven't you? ");
            }
        }.runTaskLater(KoolSMPCore.main, 3L * 20L);
        return true;
    }
}
