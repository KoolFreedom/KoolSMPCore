package eu.koolfreedom.command.impl;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.listener.impl.MuteManager;
import eu.koolfreedom.punishment.Punishment;
import eu.koolfreedom.util.FUtil;
import eu.koolfreedom.util.TimeOffset;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

@CommandParameters(name = "mute", description = "Mutes a player with optional duration and reason.", usage = "/<command> <player> [duration] [reason]")
public class MuteCommand extends KoolCommand
{
    private static final long DEFAULT_MUTE_TICKS = 5L * 60L * 20L;
    private static final long TICKS_PER_SECOND = 20L;

    private final Map<UUID, BukkitTask> muteTasks = new ConcurrentHashMap<>();

    @Override
    public void build(LiteralArgumentBuilder<CommandSourceStack> root)
    {
        root.then(literal("purge").executes(executes(ctx -> purge(sender(ctx)))))
                .then(argument("target", ArgumentTypes.player())
                        .executes(executes(ctx -> toggleMute(sender(ctx), player(ctx, "target"), null, null)))
                        .then(argument("duration", StringArgumentType.word())
                                .executes(executes(ctx -> toggleMute(sender(ctx), player(ctx, "target"),
                                        StringArgumentType.getString(ctx, "duration"), null)))
                                .then(argument("reason", StringArgumentType.greedyString())
                                        .executes(executes(ctx -> toggleMute(sender(ctx), player(ctx, "target"),
                                                StringArgumentType.getString(ctx, "duration"),
                                                StringArgumentType.getString(ctx, "reason")))))));
    }

    private void purge(CommandSender sender)
    {
        int unmuted = plugin.getMuteManager().wipeMutes();
        muteTasks.values().forEach(BukkitTask::cancel);
        muteTasks.clear();

        msg(sender, "<gray><amount> players were unmuted.",
                Placeholder.unparsed("amount", String.valueOf(unmuted)));
        FUtil.staffAction(sender, "Unmuted all players");
    }

    private void toggleMute(CommandSender sender, Player target, String durationArg, String reason)
    {
        MuteManager mum = plugin.getMuteManager();

        if (target.hasPermission("kfc.command.mute.immune"))
        {
            msg(sender, "<red>That player can't be muted.");
            return;
        }

        UUID uuid = target.getUniqueId();
        String name = target.getName();

        if (mum.isMuted(uuid))
        {
            mum.unmute(uuid);
            cancelMuteTask(uuid);
            FUtil.staffAction(sender, "Unmuted <player>", Placeholder.unparsed("player", name));
            return;
        }

        long durationTicks = parseMuteDuration(durationArg);
        if (durationTicks <= 0)
        {
            msg(sender, "<red>Invalid duration format. Example: 1d, 2h, 30m, perm");
            return;
        }

        mum.mute(uuid);
        scheduleMuteExpiration(uuid, durationTicks);
        FUtil.staffAction(sender, "Muted <player>", Placeholder.unparsed("player", name));

        plugin.getRecordKeeper().recordPunishment(Punishment.builder()
                .uuid(uuid)
                .name(name)
                .ip(FUtil.getIp(target))
                .by(sender.getName())
                .reason(reason == null ? "" : reason)
                .type("MUTE")
                .build());
    }

    private long parseMuteDuration(String durationArg)
    {
        if (durationArg == null)
        {
            return DEFAULT_MUTE_TICKS;
        }

        String dur = durationArg.toLowerCase();
        if (dur.equals("perm") || dur.equals("permanent"))
        {
            return Long.MAX_VALUE;
        }

        long ticks = TimeOffset.getOffset(dur) / 50L;
        return ticks > 0 ? ticks : -1L;
    }

    private void scheduleMuteExpiration(UUID uuid, long durationTicks)
    {
        cancelMuteTask(uuid);

        if (durationTicks == Long.MAX_VALUE)
        {
            return;
        }

        BukkitTask task = new BukkitRunnable()
        {
            @Override
            public void run()
            {
                if (!plugin.getMuteManager().isMuted(uuid))
                {
                    muteTasks.remove(uuid);
                    return;
                }

                plugin.getMuteManager().unmute(uuid);
                Player online = Bukkit.getPlayer(uuid);

                if (online != null)
                    online.sendMessage(FUtil.miniMessage("<green>Your mute has been automatically lifted after " + formatDuration(durationTicks) + "."));

                muteTasks.remove(uuid);
            }
        }.runTaskLater(plugin, Math.max(1L, durationTicks));

        muteTasks.put(uuid, task);
    }

    private void cancelMuteTask(UUID uuid)
    {
        BukkitTask task = muteTasks.remove(uuid);
        if (task != null)
        {
            task.cancel();
        }
    }

    private static String formatDuration(long ticks)
    {
        long totalSeconds = ticks / TICKS_PER_SECOND;
        long days = totalSeconds / 86400L;
        long hours = (totalSeconds % 86400L) / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;

        if (days > 0) return days + (days == 1 ? " day" : " days");
        if (hours > 0) return hours + (hours == 1 ? " hour" : " hours");
        if (minutes > 0) return minutes + (minutes == 1 ? " minute" : " minutes");
        if (seconds > 0) return seconds + (seconds == 1 ? " second" : " seconds");
        return "a moment";
    }
}
