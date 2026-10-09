package eu.koolfreedom.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.listener.impl.MuteManager;
import eu.koolfreedom.util.FUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandParameters(
        name      = "unblockcmd",
        description = "Unblocks commands for a player.",
        usage       = "/<command> <player>",
        aliases     = {"unblockcommand","unblockcommands","ubcmds","unblockcmds","ubc"}
)
public class UnblockCmdCommand extends KoolCommand
{
    @Override
    public void build(LiteralArgumentBuilder<CommandSourceStack> root)
    {
        root.then(argument("target", ArgumentTypes.player())
                .executes(executes(ctx -> unblockPlayer(sender(ctx), plugin.getMuteManager(), player(ctx, "target")))));
    }

    private void unblockPlayer(CommandSender sender, MuteManager manager, Player target)
    {
        if (!manager.isCommandsBlocked(target.getUniqueId()))
        {
            msg(sender, "<red>That player's commands aren't blocked");
            return;
        }

        manager.setCommandsBlocked(target.getUniqueId(), false);
        FUtil.staffAction(sender, "Unblocked commands for <player>", Placeholder.unparsed("player", target.getName()));
    }
}
