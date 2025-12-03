package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.listener.impl.MuteManager;
import eu.koolfreedom.util.FUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

import static org.bukkit.Bukkit.getPlayer;

@CommandParameters(name = "blockcommand", description = "Block all commands for everyone on the server, or a specific player.", usage = "/<command> <-a | purge | <player>>", aliases = {"bmcd", "blockcmd"})
public class BlockCmdCommand extends KoolCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String label, String[] args)
    {
        MuteManager manager = plugin.getMuteManager();

        if (args.length != 1)
        {
            return false;
        }

        String sub = args[0];

        if (sub.equalsIgnoreCase("purge"))
        {
            int count = manager.wipeBlockedCommands();
            FUtil.staffAction(sender, "Unblocked commands for all players.");

            for (Player p : Bukkit.getOnlinePlayers())
            {
                p.sendMessage(FUtil.miniMessage("<green>Your command block has been lifted."));
            }

            msg(sender, "<gray>Unblocked commands for " + count + " players.");
            return true;
        }

        if (sub.equalsIgnoreCase("-a"))
        {
            int count = 0;

            for (Player p : Bukkit.getOnlinePlayers())
            {
                if (!p.hasPermission("kfc.admin") && !manager.isCommandsBlocked(p.getUniqueId()))
                {
                    manager.setCommandsBlocked(p.getUniqueId(), true);
                    p.sendMessage(FUtil.miniMessage("<red>Your commands have been blocked by an admin."));
                    count++;
                }
            }

            FUtil.staffAction(sender, "Blocked commands for all non-admins.");
            msg(sender, "<gray>Blocked commands for " + count + " players.");
            return true;
        }

        Player target = getPlayer(sub);
        if (target == null)
        {
            msg(sender, playerNotFound);
            return true;
        }

        UUID uuid = target.getUniqueId();

        if (target.hasPermission("kfc.admin"))
        {
            msg(sender, "<gray>" + target.getName() + " is an admin and cannot be command-blocked.");
            return true;
        }

        if (manager.isCommandsBlocked(uuid))
        {
            msg(sender, "<red>That player's commands have already been blocked.");
            return true;
        }

        manager.setCommandsBlocked(uuid, true);
        target.sendMessage(FUtil.miniMessage("<red>Your commands have been blocked."));
        FUtil.staffAction(sender, "Blocked all commands for " + target.getName());
        msg(sender, "<gray>Blocked commands for " + target.getName() + ".");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String commandLabel, String[] args)
    {
        return args.length == 1 ? Bukkit.getOnlinePlayers().stream().map(Player::getName)
                .filter(name -> !name.equalsIgnoreCase(sender.getName())).toList() : List.of();
    }
}
