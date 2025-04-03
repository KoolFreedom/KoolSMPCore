package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.FreedomCommand;
import eu.koolfreedom.config.ConfigEntry;
import eu.koolfreedom.util.FUtil;
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
public class BanIPCommand extends FreedomCommand
{
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GRAY + "Usage: /<command> (player) [reason]");
            return true;
        }

        if (!sender.hasPermission("kf.admin")) {
            sender.sendMessage(Messages.MSG_NO_PERMS);
            return true;
        }

        Player player = Bukkit.getPlayer(args[0]);
        if (player == null) {
            sender.sendMessage(Messages.PLAYER_NOT_FOUND);
            return true;
        }

        String ip = player.getAddress().getAddress().getHostAddress(); // Get the player's IP
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
                    .append(reason);
        }

        String appeal = ConfigEntry.SERVER_WEBSITE_OR_FORUM.getString();
        message.append(ChatColor.GOLD)
                .append("\nYou may appeal by DMing one of our staff members at ")
                .append(ChatColor.RED)
                .append(appeal);

        // Ban the IP instead of the name
        Bukkit.getBanList(BanList.Type.IP).addBan(ip, reason, null, sender.getName());
        player.kickPlayer(message.toString());
        FUtil.adminAction(sender.getName(), "IP Banning " + player.getName(), true);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "discord bcast **" + sender.getName() + " - Banned an IP Address**");
        sender.sendMessage(ChatColor.GRAY + "IP Address (" + ip + ") has been banned");
        return true;
    }
}
