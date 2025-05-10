package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.config.ConfigEntry;
import eu.koolfreedom.util.FUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandParameters(name = "say", description = "Broadcast an official-looking message to the server.",
        usage = "/<command> <message>", aliases = "broadcast")
public class SayCommand extends KoolCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args)
    {
        if (args.length == 0)
        {
            return false;
        }

        FUtil.broadcast(ConfigEntry.FORMATS_SAY.getString(), Placeholder.unparsed("name", sender.getName()),
                Placeholder.unparsed("message", StringUtils.join(args, " ")));
        return true;
    }
}