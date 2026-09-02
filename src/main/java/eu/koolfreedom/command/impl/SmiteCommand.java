package eu.koolfreedom.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.punishment.Punishment;
import eu.koolfreedom.util.FUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
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
    public void build(LiteralArgumentBuilder<CommandSourceStack> root)
    {
        root.then(argument("target", ArgumentTypes.player())
                .executes(executes(ctx -> zap(player(ctx, "target"), sender(ctx), null)))
                .then(argument("reason", StringArgumentType.greedyString())
                        .executes(executes(ctx -> zap(player(ctx, "target"), sender(ctx),
                                StringArgumentType.getString(ctx, "reason"))))));
    }

    public static void zap(Player target, CommandSender sender, String reason)
    {
        FUtil.broadcast("<red><player> has been a naughty, naughty child.",
                Placeholder.unparsed("player", target.getName()));
        FUtil.broadcast("<red>Smitten by: <yellow><sender>", Placeholder.unparsed("sender", sender.getName()));

        if (reason != null)
        {
            FUtil.broadcast("<red>Reason: <yellow><reason>", Placeholder.unparsed("reason", reason));
        }

        plugin.getRecordKeeper().recordPunishment(Punishment.builder()
                .uuid(target.getUniqueId())
                .name(target.getName())
                .ip(FUtil.getIp(target))
                .by(sender.getName())
                .reason(reason)
                .type("SMITE")
                .build());

        // ZAP!
        for (int i = 0; i < 8; i++)
        {
            target.getWorld().strikeLightningEffect(target.getLocation());
        }
        target.setHealth(0);
    }
}

