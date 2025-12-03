package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@CommandParameters(name = "crash", description = "Crash people's clients.", usage = "/<command> [player]",
        aliases = {"370"})
public class CrashCommand extends KoolCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args)
    {
        if (args.length == 0)
        {
            msg(sender, "<red>You shouldn't have done that.");
            crashPlayer(playerSender);
        }
        else
        {
            Player player = Bukkit.getPlayer(args[0]);

            if (player != null)
            {
                crashPlayer(player);
                msg(sender, "<green>Your wish is my command.");
            }
            else
            {
                if (playerSender == null)
                {
                    msg(sender, "<red>We couldn't find that player. Since you're doing this from console, we can't reflect it back at you :(");
                }
                else
                {
                    msg(sender, "<red>We couldn't find that player, so we're going to do it to you instead :)");
                    crashPlayer(playerSender);
                }
            }
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String commandLabel, String[] args)
    {
        return args.length == 1 ? Bukkit.getOnlinePlayers().stream().map(Player::getName).toList() : List.of();
    }

    private void crashPlayer(Player victim)
    {
        victim.spawnParticle(Particle.ASH, victim.getLocation(), Integer.MAX_VALUE, 1, 1, 1, 1, null, true);
    }
}