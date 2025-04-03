package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.FreedomCommand;
import eu.koolfreedom.util.FUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("all")
public class CryCommand extends FreedomCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        String senderName = sender instanceof Player ? ((Player) sender).getName() : "Console";
        FUtil.bcastMsg(ChatColor.AQUA + senderName + " has started to cry :(");
        return true;
    }
}
