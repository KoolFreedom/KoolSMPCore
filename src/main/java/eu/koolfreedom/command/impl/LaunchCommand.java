package eu.koolfreedom.command.impl;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

@CommandParameters(name = "Launch", description = "Launch a player backwards 50 blocks")
public class LaunchCommand extends KoolCommand
{
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args)
    {
        // almost forgot a permission check LOL
        if (!sender.hasPermission("kfc.command.launch"))
        {
            sender.sendMessage(miniMessage.deserialize("<red>You do not have permission to use this command!</red>"));
            return true;
        }
        
        if (args.length == 0)
        {
            sender.sendMessage(miniMessage.deserialize("<red>Usage: /" + commandLabel + " <player></red>"));
            return false;
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (target == null)
        {
            sender.sendMessage(miniMessage.deserialize("<red>Player <yellow>" + args[0] + "</yellow> is not online!</red>"));
            return true;
        }
      
// make the target get LAUNCHERD!!!!!!!!!! 
        Vector backwardDirection = target.getLocation().getDirection().multiply(-1);
        Vector launchVelocity = backwardDirection.multiply(3.8);
        launchVelocity.setY(1.2);
      
        target.setVelocity(launchVelocity);

      
        // Send tuff messages.
        if (sender != target)
        {
            sender.sendMessage(miniMessage.deserialize("<green>Your wish is my command, attempting to launch: <yellow>" + target.getName() + "</yellow></green>"));
            sender.sendMessage(miniMessage.deserialize("<green>Launched: <yellow>" + target.getName() + "</yellow></green>"));
        }
        
        target.sendMessage(miniMessage.deserialize("<bold><red>Whoosh!</red></bold>"));

        return true;
    }
}

