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

@CommandParameters(name = "ban", description = "Ban someone from the server.", usage = "/<command> <player> [reason]",
        aliases = "gtfo")
public class BanCommand extends KoolCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args)
    {
        if (args.length == 0)
        {
            return false;
        }

        String reason = args.length > 1 ? String.join(" ", ArrayUtils.remove(args, 0)) : null;
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

        final Ban ban;
        if (!target.isOnline() && !target.hasPlayedBefore())
        {
            // Pre-emptive ban — player has never joined, ban by username only
            ban = Ban.fromUsername(args[0], sender.getName(), reason, TimeOffset.getOffset("1d"));
        }
        else
        {
            ban = Ban.fromPlayer(target, sender.getName(), reason, TimeOffset.getOffset("1d"));
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