package eu.koolfreedom.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.util.FUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandParameters(name = "playtime", description = "Shows your or another player's playtime", usage = "/playtime [player]")
public class PlaytimeCommand extends KoolCommand
{
    @Override
    public void build(LiteralArgumentBuilder<CommandSourceStack> root)
    {
        root.executes(executes(ctx -> playtime(sender(ctx), playerSender(ctx), null)))
                .then(argument("target", StringArgumentType.word())
                        .executes(executes(ctx -> playtime(sender(ctx), playerSender(ctx),
                                StringArgumentType.getString(ctx, "target")))));
    }

    private void playtime(CommandSender sender, Player playerSender, String targetName)
    {
        OfflinePlayer target;

        if (targetName == null)
        {
            if (playerSender == null)
            {
                msg(sender, "<red>You must specify a player from console.");
                return;
            }
            target = playerSender;
        }
        else
        {
            if (!sender.hasPermission("kfc.playtime.others"))
            {
                msg(sender, "<red>You don’t have permission to view others’ playtime.");
                return;
            }
            target = Bukkit.getOfflinePlayer(targetName);
            if (target == null || (!target.hasPlayedBefore() && !target.isOnline()))
            {
                msg(sender, playerNotFound);
                return;
            }
        }

        long playtimeSeconds = 0;
        try {
            playtimeSeconds = (long) target.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20L;
        } catch (Exception ignored) {}

        String formatted = formatDuration(playtimeSeconds);
        msg(sender, FUtil.miniMessage("<gray>" + target.getName() + " has played for <red>" + formatted + "</red>."));
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
}
