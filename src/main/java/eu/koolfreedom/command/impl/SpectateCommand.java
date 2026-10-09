package eu.koolfreedom.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@CommandParameters(name = "spectate", description = "Be like Big Brother and watch suspicious players.",
        usage = "/<command> <player>", aliases = "watch")
public class SpectateCommand extends KoolCommand
{
    @Override
    public void build(LiteralArgumentBuilder<CommandSourceStack> root)
    {
        root.then(argument("target", ArgumentTypes.player())
                .executes(executes(ctx ->
                {
                    if (isConsole(sender(ctx)))
                    {
                        msg(sender(ctx), playersOnly);
                        return;
                    }

                    Player target = player(ctx, "target");

                    if (target.getGameMode() == GameMode.SPECTATOR)
                    {
                        msg(sender(ctx), "<red>That player is also in spectator mode.");
                        return;
                    }

                    Player playerSender = playerSender(ctx);
                    assert playerSender != null;
                    playerSender.setGameMode(GameMode.SPECTATOR);
                    playerSender.setSpectatorTarget(target);
                    playerSender.teleport(target.getLocation());

                    msg(sender(ctx), "<green>Now spectating <player>.", Placeholder.unparsed("player", target.getName()));
                })));
    }
}