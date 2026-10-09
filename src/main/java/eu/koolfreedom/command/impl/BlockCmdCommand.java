package eu.koolfreedom.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.listener.impl.MuteManager;
import eu.koolfreedom.util.FUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandParameters(
        name = "blockcommand",
        description = "Block all commands for everyone on the server, or a specific player.",
        usage = "/<command> <-a | purge | <player>>",
        aliases = {"bmcd", "blockcmd"})
public class BlockCmdCommand extends KoolCommand
{
    @Override
    public void build(LiteralArgumentBuilder<CommandSourceStack> root)
    {
        root.then(literal("purge").executes(executes(ctx ->
                {
                    MuteManager manager = plugin.getMuteManager();
                    manager.wipeBlockedCommands();
                    FUtil.staffAction(sender(ctx), "Unblocked commands for all players");
                })))
                .then(literal("-a").executes(executes(ctx ->
                {
                    MuteManager manager = plugin.getMuteManager();
                    for (Player player : Bukkit.getOnlinePlayers())
                    {
                        if (!player.hasPermission("kfc.admin") && !manager.isCommandsBlocked(player.getUniqueId()))
                        {
                            manager.setCommandsBlocked(player.getUniqueId(), true);
                        }
                    }

                    FUtil.staffAction(sender(ctx), "Blocking commands for all non-admins");
                })))
                .then(argument("target", ArgumentTypes.player())
                        .executes(executes(ctx -> blockPlayer(sender(ctx), plugin.getMuteManager(), player(ctx, "target")))));
    }

    private void blockPlayer(CommandSender sender, MuteManager manager, Player target)
    {
        if (target.hasPermission("kfc.admin"))
        {
            msg(sender, "<red>That player is an admin, they cannot have their commands blocked");
            return;
        }

        if (manager.isCommandsBlocked(target.getUniqueId()))
        {
            msg(sender, "<red>That player's commands are already blocked");
            return;
        }

        manager.setCommandsBlocked(target.getUniqueId(), true);
        FUtil.staffAction(sender, "Blocking all commands for <target>",
                Placeholder.unparsed("target", target.getName()));
    }
}
