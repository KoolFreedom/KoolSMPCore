package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.FreedomCommand;
import eu.koolfreedom.util.FUtil;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class UnbanIPCommand extends FreedomCommand
{
    @SuppressWarnings("deprecation")
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String label, String[] args, boolean senderIsConsole) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GRAY + "Usage: /" + label + " (player|IP)");
            return true;
        }

        if (!sender.hasPermission("kf.admin")) {
            sender.sendMessage(Messages.MSG_NO_PERMS);
            return true;
        }

        String target = args[0];

        // Check if the input is an IP address or a player name
        if (target.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) { // Basic regex to detect IP format
            if (Bukkit.getBanList(BanList.Type.IP).isBanned(target)) {
                Bukkit.getBanList(BanList.Type.IP).pardon(target);
                sender.sendMessage(ChatColor.GREEN + "Unbanned IP: " + target);
                FUtil.adminAction(sender.getName(), "Unbanning " + target, true);
            } else {
                sender.sendMessage(ChatColor.RED + "That IP is not banned.");
            }
        } else {
            // Treat as a player name
            Player player = Bukkit.getPlayer(target);
            String ip = null;

            if (player != null) {
                ip = player.getAddress().getAddress().getHostAddress();
            } else {
                sender.sendMessage(ChatColor.RED + "Player not found. If this is an IP, try using the raw IP.");
                return true;
            }

            if (ip != null && Bukkit.getBanList(BanList.Type.IP).isBanned(ip)) {
                Bukkit.getBanList(BanList.Type.IP).pardon(ip);
                sender.sendMessage(ChatColor.GREEN + "Unbanned IP of " + target + ": " + ip);
                FUtil.adminAction(sender.getName(), "Unbanning " + target + " and IPs", true);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "discord bcast **" + sender.getName() + " - Unbanning " + target + " and IPs**");
            } else {
                sender.sendMessage(ChatColor.RED + "That player's IP is not banned.");
            }
        }

        return true;
    }
}
