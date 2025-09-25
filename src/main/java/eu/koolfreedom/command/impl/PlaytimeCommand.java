package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.util.FUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@CommandParameters(name = "playtime", description = "Shows your or another player's playtime", usage = "/playtime [player]")
public class PlaytimeCommand extends KoolCommand
{
    @Override
    public boolean run(CommandSender sender, Player player, Command cmd, String label, String[] args)
    {
        OfflinePlayer target;

        if (args.length == 0)
        {
            if (!(sender instanceof Player))
            {
                msg(sender, "<red>You must specify a player from console.");
                return true;
            }
            target = player;
        }
        else
        {
            if (!sender.hasPermission("kfc.playtime.others"))
            {
                msg(sender, "<red>You don’t have permission to view others’ playtime.");
                return true;
            }
            target = Bukkit.getOfflinePlayer(args[0]);
            if (target == null || (!target.hasPlayedBefore() && !target.isOnline()))
            {
                msg(sender, playerNotFound);
                return true;
            }
        }

        long playtimeSeconds = 0;
        try {
            playtimeSeconds = (long) target.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20L;
        } catch (Exception ignored) {}

        String formatted = formatDuration(playtimeSeconds);
        msg(sender, FUtil.miniMessage("<gray>" + target.getName() + " has played for <red>" + formatted + "</red>."));
        return true;
    }

    private String formatDuration(long totalSeconds)
    {
        if (totalSeconds <= 0) return "0s";
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long secs = totalSeconds % 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (secs > 0) sb.append(secs).append("s");
        return sb.toString().trim();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String commandLabel, String[] args)
    {
        return args.length == 1 && sender.hasPermission("kfc.playtime.others")
                ? Bukkit.getOnlinePlayers().stream().map(Player::getName).toList()
                : List.of();
    }
}
