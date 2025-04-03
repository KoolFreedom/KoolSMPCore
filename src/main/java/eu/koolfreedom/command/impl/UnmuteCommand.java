package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.FreedomCommand;
import eu.koolfreedom.listener.PunishmentListener;
import eu.koolfreedom.util.FUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class UnmuteCommand extends FreedomCommand
{
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length == 0)
        {
            sender.sendMessage(ChatColor.GRAY + "Usage: /<command> [player]");
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

        if (!PunishmentListener.isMuted(player))
        {
            sender.sendMessage(ChatColor.RED + "Player is not muted!");
            return true;
        }

        PunishmentListener.removeMute(player);
        FUtil.adminAction(sender.getName(), "Unmuting " + player.getName(), true);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "discord bcast **" + sender.getName() + " - Unuting " + player.getName() + "**");
        return true;
    }
}
