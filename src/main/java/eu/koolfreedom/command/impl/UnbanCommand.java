package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.FreedomCommand;
import eu.koolfreedom.util.FUtil;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UnbanCommand extends FreedomCommand
{
    @Override
    @SuppressWarnings("deprecation")
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole) {
        if (args.length == 0)
        {
            sender.sendMessage(ChatColor.GRAY + "Usage: /<command> (player)");
            return false;
        }

        if (!sender.hasPermission("kf.admin"))
        {
            sender.sendMessage(Messages.MSG_NO_PERMS);
            return true;
        }

        OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);

        if (!player.isBanned())
        {
            sender.sendMessage(ChatColor.GRAY + "That player is not banned");
            return true;
        }

        Bukkit.getBanList(BanList.Type.NAME).pardon(player.getName());
        FUtil.adminAction(sender.getName(), "Unbanning " + player.getName(), true);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "discord bcast **" + sender.getName() + " - Unbanning " + player.getName() + "**");
        return true;
    }
}
