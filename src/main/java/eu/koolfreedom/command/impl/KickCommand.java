package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.punishment.Punishment;
import eu.koolfreedom.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerKickEvent;

import java.util.List;

@CommandParameters(name = "kick", description = "Kick someone from the server.", usage = "/<command> <player> <reason>")
public class KickCommand extends KoolCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args)
    {
        if (args.length == 0)
        {
            return false;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null)
        {
            msg(sender, playerNotFound);
            return true;
        }

        String reason = args.length >= 2 ? String.join(" ", ArrayUtils.remove(args, 0)) : null;

        plugin.recordKeeper.recordPunishment(Punishment.builder()
                .uuid(target.getUniqueId())
                .name(target.getName())
                .ip(FUtil.getIp(target))
                .type("KICK")
                .reason(reason)
                .build());

        target.kick(FUtil.miniMessage("<red>You have been kicked from the server." +
                        "<newline>Kicked by: <yellow><sender></yellow><reason_if_present>",
                Placeholder.unparsed("sender", sender.getName()),
                Placeholder.component("reason_if_present", reason != null ?
                        FUtil.miniMessage("<newline>Reason: <yellow><reason></yellow>",
                                Placeholder.unparsed("reason", reason)) :
                        Component.empty())), PlayerKickEvent.Cause.KICK_COMMAND);

        FUtil.staffAction(sender, "Kicked <player>" + (reason != null ? ", Reason: <white><reason></white>" : ""),
                Placeholder.unparsed("player", target.getName()),
                Placeholder.unparsed("reason", reason != null ? reason : ""));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String commandLabel, String[] args)
    {
        return args.length == 1 ? Bukkit.getOnlinePlayers().stream().map(Player::getName)
                .filter(name -> !name.equalsIgnoreCase(sender.getName())).toList() : List.of();
    }
}
