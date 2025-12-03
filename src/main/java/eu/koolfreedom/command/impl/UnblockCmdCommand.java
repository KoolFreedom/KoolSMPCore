package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.listener.impl.MuteManager;
import eu.koolfreedom.util.FUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@CommandParameters(
        name      = "unblockcmd",
        description = "Unblocks commands for a player.",
        usage       = "/<command> <player>",
        aliases     = {"unblockcommand","unblockcommands","ubcmds","unblockcmds","ubc"}
)
public class UnblockCmdCommand extends KoolCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String label, String[] args)
    {
        if (args.length != 1)
        {
            return false;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null)
        {
            msg(sender, playerNotFound);
            return true;
        }

        MuteManager manager = plugin.getMuteManager();

        if (!manager.isCommandsBlocked(target.getUniqueId()))
        {
            msg(sender, "<red>That player's commands aren't blocked.");
            return true;
        }

        manager.setCommandsBlocked(target.getUniqueId(), false);

        FUtil.staffAction(sender,
                "Unblocked commands for <player>",
                Placeholder.unparsed("player", target.getName()));

        msg(sender,
                "<gray>Unblocked commands for <player>",
                Placeholder.unparsed("player", target.getName()));

        target.sendMessage(FUtil.miniMessage("<green>Your command block has been lifted."));

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String commandLabel, String[] args)
    {
        return args.length == 1 ? Bukkit.getOnlinePlayers().stream().map(Player::getName)
                .filter(name -> !name.equalsIgnoreCase(sender.getName())).toList() : List.of();
    }
}
