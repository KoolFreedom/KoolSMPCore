package eu.koolfreedom.command.impl;

import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.config.ConfigEntry;
import eu.koolfreedom.util.FUtil;
import org.apache.commons.lang.StringUtils;
import org.bukkit.BanList;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("deprecation")
public class ObliterateCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args)
    {
        if (!sender.hasPermission("kf.senior"))
        {
            sender.sendMessage(Messages.MSG_NO_PERMS);
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(KoolSMPCore.main.mmDeserialize("<red>Usage: /" + s + " <player> [reason]"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Messages.PLAYER_NOT_FOUND);
            return true;
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

        String ip = target.getAddress().getAddress().getHostAddress(); // Get the player's IP
        if (ip == null) {
            sender.sendMessage(ChatColor.RED + "Could not retrieve player's IP address.");
            return true;
        }

        StringBuilder message = new StringBuilder()
                .append(ChatColor.GOLD)
                .append("You've been banned!")
                .append("\nBanned by: ")
                .append(ChatColor.RED)
                .append(sender.getName());

        String reason = Messages.NO_REASON;
        if (args.length > 1) {
            reason = StringUtils.join(args, " ", 1, args.length);
            message.append(ChatColor.GOLD)
                    .append("\nReason: ")
                    .append(ChatColor.RED)
                    .append("You've met with a terrible fate, haven't you, " + target.getName() + "?" + " (" +  reason + ")");
        }

        String appeal = ConfigEntry.SERVER_WEBSITE_OR_FORUM.getString();
        message.append(ChatColor.GOLD)
                .append("\nYou may appeal by DMing one of our staff members at ")
                .append(ChatColor.RED)
                .append(appeal);



        String finalReason = reason; // same with doom, what the fuck intellij....
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
                Bukkit.getBanList(BanList.Type.IP).addBan(ip, finalReason, null, sender.getName());
                target.kickPlayer(message.toString());
                FUtil.adminAction(sender.getName(), "Sending " + target.getName() + " to the Moon", true);
            }
        }.runTaskLater(KoolSMPCore.main, 3L * 20L);
        return true;
    }
}
