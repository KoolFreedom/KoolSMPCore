package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.FreedomCommand;
import org.apache.commons.lang.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import eu.koolfreedom.util.FUtil;
import org.bukkit.entity.Player;

public class AdminChatCommand extends FreedomCommand
{
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (!sender.hasPermission("kf.adminchat"))
        {
            sender.sendMessage(Messages.MSG_NO_PERMS);
            return true;
        }
        if (args.length == 0)
        {
            sender.sendMessage(Messages.MISSING_ARGS);
            return true;
        }
        String message = StringUtils.join(args, " ");
        FUtil.adminChat(sender, message);
        return true;
    }
}
