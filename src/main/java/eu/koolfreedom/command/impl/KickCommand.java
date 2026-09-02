package eu.koolfreedom.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.punishment.Punishment;
import eu.koolfreedom.util.FUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerKickEvent;

@CommandParameters(name = "kick", description = "Kick someone from the server.", usage = "/<command> <player> [reason]")
public class KickCommand extends KoolCommand
{
    @Override
    public void build(LiteralArgumentBuilder<CommandSourceStack> root)
    {
        root.then(argument("target", ArgumentTypes.player())
                .executes(executes(ctx -> kick(sender(ctx), player(ctx, "target"), null)))
                .then(argument("reason", StringArgumentType.greedyString())
                        .executes(executes(ctx -> kick(sender(ctx), player(ctx, "target"),
                                StringArgumentType.getString(ctx, "reason"))))));
    }

    private void kick(CommandSender sender, Player target, String reason)
    {
        plugin.getRecordKeeper().recordPunishment(Punishment.builder()
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
                        Component.empty())), PlayerKickEvent.Cause.KICKED);

        FUtil.staffAction(sender, "Kicked <player>" + (reason != null ? ", Reason: <white><reason></white>" : ""),
                Placeholder.unparsed("player", target.getName()),
                Placeholder.unparsed("reason", reason != null ? reason : ""));
    }
}
