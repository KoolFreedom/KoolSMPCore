package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.FreedomCommand;
import eu.koolfreedom.util.FUtil;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SayCommand extends FreedomCommand
{

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole) {
        if (args.length == 0)
        {
            return false;
        }

        String message = StringUtils.join(args, " ");

        FUtil.bcastMsg(String.format("[Server:%s] %s", sender.getName(), message), ChatColor.LIGHT_PURPLE);
        // This will only execute if there is DiscordSRV installed, I will not make this a dependency, but be warned
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "discord bcast " + (String.format("[Server:%s] %s", sender.getName(), message)));

        return true;
    }
}