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

@CommandParameters(name = "smite", description = "\"Kindly\" correct a misbehaving player.",
        usage = "/<command> <player> [reason]", aliases = {"bitchslap"})
public class SmiteCommand extends KoolCommand
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


        broadcast("<red><player> has been a naughty, naughty child.",
                Placeholder.unparsed("player", target.getName()));
        broadcast(" <red>Smitten by: <yellow><sender>", Placeholder.unparsed("sender", sender.getName()));

        if (args.length >= 2)
        {
            String reason = String.join(" ", ArrayUtils.remove(args, 0));
            broadcast(" <red>Reason: <yellow><reason>", Placeholder.unparsed("reason", reason));
        }

        plugin.recordKeeper.recordPunishment(Punishment.builder()
                .uuid(target.getUniqueId())
                .name(target.getName())
                .ip(FUtil.getIp(target))
                .type("SMITE")
                .build());

        // ZAP!
        for (int i = 0; i < 8; i++)
        {
            target.getWorld().strikeLightningEffect(target.getLocation());
        }
        target.setHealth(0);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String commandLabel, String[] args)
    {
        return args.length == 1 ? Bukkit.getOnlinePlayers().stream().map(Player::getName).toList() : List.of();
    }
}

