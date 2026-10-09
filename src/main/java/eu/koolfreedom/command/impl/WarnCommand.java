package eu.koolfreedom.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.punishment.Punishment;
import eu.koolfreedom.util.FUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandParameters(name = "warn", description = "Formally warn a player.", usage = "/<command> <player> <reason>")
public class WarnCommand extends KoolCommand
{
    @Override
    public void build(LiteralArgumentBuilder<CommandSourceStack> root)
    {
        root.then(argument("target", ArgumentTypes.player())
                .then(argument("reason", StringArgumentType.greedyString())
                        .executes(executes(ctx -> warn(sender(ctx), player(ctx, "target"),
                                StringArgumentType.getString(ctx, "reason"))))));
    }

    private void warn(CommandSender sender, Player target, String reason)
    {
        if (target.hasPermission("kfc.admin"))
        {
            msg(sender, "<red>That player can't be warned.");
            return;
        }

        plugin.getRecordKeeper().recordPunishment(Punishment.builder()
                .uuid(target.getUniqueId())
                .name(target.getName())
                .ip(FUtil.getIp(target))
                .type("WARN")
                .build());

        msg(target, "<red>You have been warned for the following reason: <yellow><reason></yellow>",
                Placeholder.unparsed("reason", reason));

        FUtil.staffAction(sender, "Warned <player>", Placeholder.unparsed("player", target.getName()));
    }
}
