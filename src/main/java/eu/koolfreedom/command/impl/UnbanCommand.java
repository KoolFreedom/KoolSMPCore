package eu.koolfreedom.command.impl;

import eu.koolfreedom.discord.DiscordLogger;
import eu.koolfreedom.discord.StaffActionType;
import eu.koolfreedom.util.FUtil;
import eu.koolfreedom.util.StaffActionLogger;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class UnbanCommand implements CommandExecutor
{
    @Override
    @SuppressWarnings("deprecation")
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args)
    {
        if (!sender.hasPermission("kf.admin"))
        {
            sender.sendMessage(Messages.MSG_NO_PERMS);
            return true;
        }

        if (args.length == 0)
        {
            sender.sendMessage(ChatColor.GRAY + "Usage: /<command> (player)");
            return false;
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
        DiscordLogger.sendStaffAction(StaffActionType.UNBAN, sender.getName(), player.getName(), null);
        StaffActionLogger.log(StaffActionType.UNBAN, sender.getName(), player.getName(), null);
        return true;
    }
}
