package eu.koolfreedom.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import static org.bukkit.Bukkit.getPlayer;


public class HugCommand {
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length == 0)
        {
            return false;
        }
        Player player = getPlayer(args[0]);
        if (player == null)
        {
            sender.sendMessage(ChatColor.RED + "Player not found!");
            return true;
        }
        Bukkit.broadcastMessage(ChatColor.AQUA + (playerSender.getName()) + ChatColor.AQUA + " has given " + (player.getName()) + ChatColor.AQUA + " a warm hug!");
        return true;
    }
}