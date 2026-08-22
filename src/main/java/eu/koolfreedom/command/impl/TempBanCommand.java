package eu.koolfreedom.command.impl;

import eu.koolfreedom.banning.Ban;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.punishment.Punishment;
import eu.koolfreedom.util.FUtil;
import eu.koolfreedom.util.TimeOffset;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@CommandParameters(name = "tban", description = "Ban a player with optional duration and reason.",
        aliases = {"tempban"}, usage = "/<command> <player> [duration] [reason]")
public class TempBanCommand extends KoolCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args)
    {
        if (args.length == 0) return false;

        long duration;
        String reason = null;

        if (args.length >= 2)
        {
            String dur = args[1].toLowerCase();
            if (dur.equals("perm") || dur.equals("permanent"))
            {
                duration = Long.MAX_VALUE;
                reason = args.length > 2 ? String.join(" ", ArrayUtils.subarray(args, 2, args.length)) : null;
            }
            else
            {
                duration = TimeOffset.getOffset(dur);
                if (duration <= 0)
                {
                    msg(sender, "<red>Invalid duration format. Example: 1d, 2h, 30m, perm");
                    return true;
                }
                reason = args.length > 2 ? String.join(" ", ArrayUtils.subarray(args, 2, args.length)) : null;
            }
        }
        else
        {
            duration = TimeOffset.getOffset("1d");
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

        Ban ban;
        if (!target.isOnline() && !target.hasPlayedBefore())
        {
            // Pre-emptive ban — player has never joined, ban by username only
            ban = Ban.fromUsername(args[0], sender.getName(), reason, duration);
        }
        else
        {
            ban = Ban.fromPlayer(target, sender.getName(), reason, duration);
        }

        boolean success = plugin.getBanManager().addBan(ban);
        if (!success)
        {
            msg(sender, "<red>That user is already banned.");
            return true;
        }

        String displayName = target.getName() != null ? target.getName() : args[0];
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

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String commandLabel, String[] args)
    {
        return args.length == 1 ? Bukkit.getOnlinePlayers().stream().map(Player::getName)
                .filter(name -> !name.equalsIgnoreCase(sender.getName())).toList() : List.of();
    }
}