package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
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
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args)
    {
        if (playerSender == null)
        {
            msg(sender, playersOnly);
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null)
        {
            msg(sender, playerNotFound);
            return true;
        }

        if (target.getGameMode() == GameMode.SPECTATOR)
        {
            msg(sender, "<red>That player is also in spectator mode.");
            return true;
        }

        playerSender.setGameMode(GameMode.SPECTATOR);
        playerSender.setSpectatorTarget(target);
        playerSender.teleport(target.getLocation());

        msg(sender, "<green>Now spectating <player>.", Placeholder.unparsed("player", target.getName()));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String commandLabel, String[] args)
    {
        return args.length == 1 ? Bukkit.getOnlinePlayers().stream().filter(player -> player.getGameMode() != GameMode.SPECTATOR)
                .map(Player::getName).toList() : List.of();
    }
}