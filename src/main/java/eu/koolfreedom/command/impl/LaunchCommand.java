package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.command.annotation.CommandParameters;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

@CommandParameters(name = "launch", description = "Launch a player backwards 50 blocks", usage = "/launch <player>")
public class LaunchCommand extends KoolCommand
{
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

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
            msg(sender, playerNotFound)
            return true;
        }
      
        Vector backwardDirection = target.getLocation().getDirection().multiply(-1);
        Vector launchVelocity = backwardDirection.multiply(3.8);
        launchVelocity.setY(1.2);
      
        target.setVelocity(launchVelocity);

      
        if (sender != target)
        {
            msg(sender, "<green>Your wish is my command.");
            msg(sender, "<green>Player has been launched"
        }
        
        msg(target, "<red>Woosh!");
        return true;
    }
}

