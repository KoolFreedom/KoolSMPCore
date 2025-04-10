package eu.koolfreedom.command.impl;

import eu.koolfreedom.config.ConfigEntry;
import eu.koolfreedom.discord.DiscordLogger;
import eu.koolfreedom.discord.StaffActionType;
import eu.koolfreedom.util.FUtil;
import eu.koolfreedom.util.StaffActionLogger;
import org.apache.commons.lang.StringUtils;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("deprecation")
public class BanCommand implements CommandExecutor
{
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args)
    {
        if (!sender.hasPermission("kf.admin")) {
            sender.sendMessage(Messages.MSG_NO_PERMS);
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.GRAY + "Usage: /<command> (player) [reason]");
            return true;
        }

        Player player = Bukkit.getPlayer(args[0]);
        if (player == null) {
            sender.sendMessage(Messages.PLAYER_NOT_FOUND);
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
                    .append(reason);
        }

        String appeal = ConfigEntry.SERVER_WEBSITE_OR_FORUM.getString();
        message.append(ChatColor.GOLD)
                .append("\nYou may appeal by DMing one of our staff members at ")
                .append(ChatColor.RED)
                .append(appeal);

        Bukkit.getBanList(BanList.Type.NAME).addBan(player.getName(), reason, null, sender.getName());
        player.kickPlayer(message.toString());
        FUtil.adminAction(sender.getName(), "Banning " + player.getName(), true);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "discord bcast **" + sender.getName() + " - Banning " + player.getName() + "**");
        // Log the ban
        DiscordLogger.sendStaffAction(StaffActionType.BAN, sender.getName(), player.getName(), reason);
        StaffActionLogger.log(StaffActionType.BAN, sender.getName(), player.getName(), reason);
        return true;
    }
}
