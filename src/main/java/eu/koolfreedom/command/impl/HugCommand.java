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

@CommandParameters(name = "hug", description = "Hug someone.", usage = "/<command> <player>")
public class HugCommand extends KoolCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args)
    {
        if (args.length != 1)
        {
            return false;
        }

        Player player = FUtil.getPlayer(args[0], sender.hasPermission("kfc.command.see_vanished_players"));
        if (player == null)
        {
            msg(sender, playerNotFound);
            return true;
        }

        if (player.equals(playerSender))
        {
            msg(sender, "<red>You can't hug yourself.");
            return true;
        }

        broadcast("<aqua><sender> gave <target> a warm hug!",
                Placeholder.unparsed("sender", sender.getName()),
                Placeholder.unparsed("target", player.getName()));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String commandLabel, String[] args)
    {
        return args.length == 1 ? FUtil.getPlayerList(sender.hasPermission("kfc.command.see_vanished_players"))
            .stream().filter(name -> !name.equalsIgnoreCase(sender.getName())).toList() : List.of();
    }
}
