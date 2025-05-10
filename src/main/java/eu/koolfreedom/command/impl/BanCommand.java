package eu.koolfreedom.command.impl;

import eu.koolfreedom.banning.Ban;
import eu.koolfreedom.command.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.punishment.Punishment;
import eu.koolfreedom.util.FUtil;
import eu.koolfreedom.banning.BanType;
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

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.isOnline() && !target.hasPlayedBefore())
        {
            msg(sender, playerNotFound);
            return true;
        }

        final Ban ban = Ban.fromPlayer(target, sender.getName(), args.length > 1 ? String.join(" ", ArrayUtils.remove(args, 0)) : null, BanType.BAN);
        boolean success = plugin.banManager.addBan(ban);

        if (!success && !target.isOnline())
        {
            msg(sender, "<red>That user is already permanently banned.");
            return true;
        }

        FUtil.staffAction(sender, "Banned <player>", Placeholder.unparsed("player", target.getName() != null ? target.getName() : args[0]));
        plugin.recordKeeper.recordPunishment(Punishment.fromBan(ban));

        if (target instanceof Player online)
        {
            // Now for the fun part...
            for (int i = 0; i < 4; i++)
            {
                online.getWorld().strikeLightning(online.getLocation());
            }
            online.setHealth(0);

            // We had our fun, they're gone
            online.kick(ban.getKickMessage());

            // Just for good measure...
            Bukkit.getOnlinePlayers().stream().filter(player -> FUtil.getIp(player).equalsIgnoreCase(FUtil.getIp(online))).forEach(player ->
            {
                // ZAP, and they're gone
                for (int i = 0; i < 4; i++)
                {
                    player.getWorld().strikeLightning(player.getLocation());
                }
                player.setHealth(0);
                player.kick(ban.getKickMessage());
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
