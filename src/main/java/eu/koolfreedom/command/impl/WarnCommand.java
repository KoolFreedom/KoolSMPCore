package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.punishment.Punishment;
import eu.koolfreedom.util.FUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@CommandParameters(name = "warn", description = "Formally warn a player.", usage = "/<command> <player> <reason>")
public class WarnCommand extends KoolCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args)
    {
        if (args.length <= 1)
        {
            return false;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null)
        {
            msg(sender, playerNotFound);
            return true;
        }

        if (target.hasPermission("kfc.command.warn.immune"))
        {
            msg(sender, "<red>That player can't be warned.");
            return true;
        }

        String reason = String.join(" ", ArrayUtils.remove(args, 0));

        // Record it to logs
        plugin.recordKeeper.recordPunishment(Punishment.builder()
                .uuid(target.getUniqueId())
                .name(target.getName())
                .ip(FUtil.getIp(target))
                .type("WARN")
                .build());

        // Let the user know they are being warned
        msg(target, "<red>You have been warned for the following reason: <yellow><reason></yellow>",
                Placeholder.unparsed("reason", reason));

        // Broadcast it
        FUtil.staffAction(sender, "Warned <player>", Placeholder.unparsed("player", target.getName()));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String commandLabel, String[] args)
    {
        return args.length == 1 ? Bukkit.getOnlinePlayers().stream().filter(player -> !player.hasPermission("kfc.command.warn.immune"))
                .map(Player::getName).toList() : List.of();
    }
}
