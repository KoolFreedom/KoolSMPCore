package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.FreedomCommand;
import eu.koolfreedom.util.FUtil;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class WarnCommand extends FreedomCommand
{
    @Override
    @SuppressWarnings("deprecation")
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length == 0)
        {
            sender.sendMessage(ChatColor.GRAY + "Usage: /<command> (player) [reason]");
            return true;
        }

        if (!sender.hasPermission("kf.admin"))
        {
            sender.sendMessage(Messages.MSG_NO_PERMS);
            return true;
        }

        Player player = Bukkit.getPlayer(args[0]);
        if (player == null)
        {
            sender.sendMessage(Messages.PLAYER_NOT_FOUND);
            return true;
        }

        String reason = Messages.NO_REASON;
        if (args.length > 1)
        {
            reason = StringUtils.join(args, " ", 1, args.length);
        }

        FUtil.adminAction(sender.getName(), "Warning " + player.getName(), true);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "discord bcast **" + sender.getName() + " - Warning " + player.getName() + "**");
        player.sendMessage(ChatColor.DARK_RED + "You have been warned for the following reason: " + reason);
        return true;
    }
}
