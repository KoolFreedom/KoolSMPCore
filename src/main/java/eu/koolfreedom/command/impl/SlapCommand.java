package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.util.FUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@CommandParameters(name = "slap", description = "Slap the shit out of someone.", usage = "/<command> <player>")
public class SlapCommand extends KoolCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args)
    {
        if (args.length != 1)
        {
            return false;
        }

        Player target = FUtil.getPlayer(args[0], sender.hasPermission("kfc.command.see_vanished_players"));
        if (target == null)
        {
            msg(sender, playerNotFound);
            return true;
        }

        if (target.equals(playerSender))
        {
            playerSender.damage(Math.max(playerSender.getHealth() / 4, 1.0));
            msg(sender, "<red>Ouch! That looks like it must have hurt.");
            return true;
        }

        broadcast("<aqua><sender> gave <player> a nice slap to the face!",
                Placeholder.unparsed("sender", sender.getName()),
                Placeholder.unparsed("player", target.getName()));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String commandLabel, String[] args)
    {
        return args.length == 1 ? FUtil.getPlayerList(sender.hasPermission("kfc.command.see_vanished_players"))
                .stream().filter(name -> !name.equalsIgnoreCase(sender.getName())).toList() : List.of();
    }
}
