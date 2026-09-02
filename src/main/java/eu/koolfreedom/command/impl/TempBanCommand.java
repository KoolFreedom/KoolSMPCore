package eu.koolfreedom.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.koolfreedom.banning.Ban;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.punishment.Punishment;
import eu.koolfreedom.util.FUtil;
import eu.koolfreedom.util.TimeOffset;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandParameters(name = "tban", description = "Ban a player with optional duration and reason.",
        aliases = {"tempban"}, usage = "/<command> <player> [duration] [reason]")
public class TempBanCommand extends KoolCommand
{
    @Override
    public void build(LiteralArgumentBuilder<CommandSourceStack> root)
    {
        root.then(argument("target", StringArgumentType.word())
                .executes(executes(ctx -> tempBan(sender(ctx), StringArgumentType.getString(ctx, "target"), null, null)))
                .then(argument("duration", StringArgumentType.word())
                        .executes(executes(ctx -> tempBan(sender(ctx), StringArgumentType.getString(ctx, "target"),
                                StringArgumentType.getString(ctx, "duration"), null)))
                        .then(argument("reason", StringArgumentType.greedyString())
                                .executes(executes(ctx -> tempBan(sender(ctx), StringArgumentType.getString(ctx, "target"),
                                        StringArgumentType.getString(ctx, "duration"),
                                        StringArgumentType.getString(ctx, "reason")))))));
    }

    private void tempBan(CommandSender sender, String targetName, String durationArg, String reason)
    {
        long duration;

        if (durationArg != null)
        {
            String dur = durationArg.toLowerCase();
            if (dur.equals("perm") || dur.equals("permanent"))
            {
                duration = Long.MAX_VALUE;
            }
            else
            {
                duration = TimeOffset.getOffset(dur);
                if (duration <= 0)
                {
                    msg(sender, "<red>Invalid duration format. Example: 1d, 2h, 30m, perm");
                    return;
                }
            }
        }
        else
        {
            duration = TimeOffset.getOffset("1d");
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        Ban ban;
        if (!target.isOnline() && !target.hasPlayedBefore())
        {
            // Pre-emptive ban — player has never joined, ban by username only
            ban = Ban.fromUsername(targetName, sender.getName(), reason, duration);
        }
        else
        {
            ban = Ban.fromPlayer(target, sender.getName(), reason, duration);
        }

        boolean success = plugin.getBanManager().addBan(ban);
        if (!success)
        {
            msg(sender, "<red>That user is already banned.");
            return;
        }

        String displayName = target.getName() != null ? target.getName() : targetName;
        FUtil.staffAction(sender, "Banned <player>", Placeholder.unparsed("player", displayName));
        plugin.getRecordKeeper().recordPunishment(Punishment.fromBan(ban));

        if (target instanceof Player online)
        {
            for (int i = 0; i < 4; i++)
                online.getWorld().strikeLightning(online.getLocation());
            online.setHealth(0);
            online.kick(ban.getKickMessage());

            Bukkit.getOnlinePlayers().stream()
                    .filter(p -> FUtil.getIp(p).equalsIgnoreCase(FUtil.getIp(online)))
                    .forEach(p ->
                    {
                        for (int i = 0; i < 4; i++)
                            p.getWorld().strikeLightning(p.getLocation());
                        p.setHealth(0);
                        p.kick(ban.getKickMessage());
                    });
        }
    }
}