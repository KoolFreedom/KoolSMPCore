package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.listener.impl.LockupManager;
import eu.koolfreedom.util.FUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@CommandParameters(
        name        = "lockup",
        description = "Toggle lock‑up for a player (opens their inventory and immobilises them).",
        usage       = "/<command> <player>",
        aliases     = {"lock", "lockplayer"}
)
public class LockupCommand extends KoolCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command command, String label, String[] args)
    {
        if (args.length != 1)
            return false;

        Player target = FUtil.getPlayer(args[0], true);
        if (target == null)
        {
            msg(sender, playerNotFound);
            return true;
        }

        LockupManager lm   = plugin.getLockupManager();
        boolean nowLocked  = lm.toggle(target);

        FUtil.staffAction(sender,
                (nowLocked ? "Locked‑up " : "Unlocked ") + "<player>",
                Placeholder.unparsed("player", target.getName()));

        msg(sender, "<gray>" + target.getName() + " is now "
                + (nowLocked ? "<red>locked‑up" : "<green>free") + ".");

        if (nowLocked) target.openInventory(target.getInventory());
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command,
                                    String label, String[] args)
    {
        return args.length == 1
                ? Bukkit.getOnlinePlayers().stream().map(Player::getName).toList()
                : List.of();
    }
}
