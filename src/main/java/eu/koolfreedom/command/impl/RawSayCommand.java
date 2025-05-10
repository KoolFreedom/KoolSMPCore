package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandParameters(name = "rawsay", description = "Broadcast a MiniMessage-formatted message to the server.",
        usage = "/<command> <message>", aliases = {"bcraw"})
public class RawSayCommand extends KoolCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args)
    {
        if (args.length == 0)
        {
            return false;
        }

        broadcast(StringUtils.join(args, " "));
        return true;
    }
}
