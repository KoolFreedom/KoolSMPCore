package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.freeze.FreezeManager;
import eu.koolfreedom.util.FUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

@CommandParameters(name = "freeze", description = "Freeze players", usage = "/freeze <player>")
public class FreezeCommand extends KoolCommand
{
    private final FreezeManager manager = plugin.getFreezeManager();

    @Override
    public boolean run(CommandSender sender, Player player, Command command, String label, String[] args) {
        if (args.length == 0) return false;

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null)
        {
            msg(sender, playerNotFound);
            return true;
        }

        UUID uuid = target.getUniqueId();

        if (manager.isFrozen(target))
        {
            manager.unfreeze(target);
            FUtil.staffAction(sender, "Unfroze " + target.getName());
            msg(sender, "<gray>Unfroze " + target.getName());
            target.sendMessage(FUtil.miniMessage("<gray>You have been <aqua>unfrozen!"));
            plugin.getAutoUndoManager().cancelAutoUnfreeze(uuid);
            return true;
        }

        manager.freeze(target);
        FUtil.staffAction(sender, "Froze <player>", Placeholder.unparsed("player", target.getName()));
        msg(sender, "<gray>Froze <player>", Placeholder.unparsed("player", target.getName()));
        target.sendMessage(FUtil.miniMessage("<gray>You have been <aqua>frozen!"));
        plugin.getAutoUndoManager().scheduleAutoUnfreeze(target);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String commandLabel, String[] args)
    {
        return args.length == 1 ? Bukkit.getOnlinePlayers().stream().map(Player::getName)
                .filter(name -> !name.equalsIgnoreCase(sender.getName())).toList() : List.of();
    }
}
