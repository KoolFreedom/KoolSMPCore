package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.FreedomCommand;
import eu.koolfreedom.util.FUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class PatCommand extends FreedomCommand
{

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length == 0)
        {
            return false;
        }
        Player player = getPlayer(args[0]);
        if (player == null)
        {
            msg(Messages.PLAYER_NOT_FOUND);
            return true;
        }
        FUtil.bcastMsg(ChatColor.AQUA + (playerSender.getName()) + ChatColor.AQUA + " gave " + (player.getName()) + ChatColor.AQUA + " a pat on the head");
        return true;
    }
}