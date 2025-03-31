package eu.koolfreedom.command;

import eu.koolfreedom.listener.PunishmentListener;
import eu.koolfreedom.util.FUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class MuteCommand implements CommandExecutor
{

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender.hasPermission("kf.admin"))) {
            sender.sendMessage(Messages.MSG_NO_PERMS);
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /mute <player> [reason]");
            return true;
        }

        Player player = Bukkit.getPlayer(args[0]);
        if (player == null)
        {
            sender.sendMessage(Messages.PLAYER_NOT_FOUND);
            return true;
        }

        if (PunishmentListener.isMuted(player))
        {
            sender.sendMessage(ChatColor.RED + "Player is already muted!");
            return true;
        }

        String reason = args.length > 1 ? "(" + String.join(" ", Arrays.copyOfRange(args, 1, args.length)) + ")" : "";
        PunishmentListener.addMute(player);

        FUtil.adminAction(sender.getName(), "Muting " + player.getName() + " | Reason: " + reason, true);
        sender.sendMessage(ChatColor.GRAY + "You have been muted | Reason " + reason);
        return true;
    }
}
