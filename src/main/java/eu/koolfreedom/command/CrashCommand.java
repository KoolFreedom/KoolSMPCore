package eu.koolfreedom.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@SuppressWarnings("deprecation")
public class CrashCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if the command sender has permission
        if (!sender.hasPermission("kf.exec")) {
            sender.sendMessage(Messages.MSG_NO_PERMS);
            return true;
        }

        // Check if the command is run by a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.ONLY_IN_GAME);
            return true;
        }

        Player executor = (Player) sender;

        if (args.length == 0) {
            // Crash all players except the executor
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.equals(executor)) {
                    crashPlayer(player);
                }
            }
            executor.sendMessage(Messages.CRASHED_ALL);
        } else if (args.length == 1) {
            // Crash a specific player
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                executor.sendMessage(Messages.PLAYER_NOT_FOUND);
                return true;
            }
            if (target.equals(executor)) {
                executor.sendMessage(Messages.CRASH_SELF);
                return true;
            }

            crashPlayer(target);
            executor.sendMessage(ChatColor.GREEN + "Crashed player: " + target.getName());
        } else {
            executor.sendMessage(ChatColor.RED + "Usage: /crash [player]");
        }

        return true;
    }

    private void crashPlayer(Player player) {
        // Send a particle overload to crash the player's client
        String command = "execute at " + player.getName() + " run particle ash ~ ~ ~ 1 1 1 1 2147483647 force " + player.getName();
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

        player.sendMessage(Messages.MSG_CRASHED);
    }
}