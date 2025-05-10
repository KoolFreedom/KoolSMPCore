package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import eu.koolfreedom.util.FUtil;
import org.bukkit.entity.Player;

@CommandParameters(name = "adminchat", description = "Speak with other staff members", aliases = {"o", "oc", "ac"})
public class AdminChatCommand extends KoolCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args)
    {
        if (args.length == 0)
        {
            return false;
        }

        FUtil.adminChat(sender, StringUtils.join(args, " "));
        return true;
    }
}
