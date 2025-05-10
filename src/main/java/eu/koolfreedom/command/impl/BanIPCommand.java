package eu.koolfreedom.command.impl;

import com.google.common.net.InetAddresses;
import eu.koolfreedom.banning.Ban;
import eu.koolfreedom.banning.BanType;
import eu.koolfreedom.command.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.punishment.Punishment;
import eu.koolfreedom.util.FUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@CommandParameters(name = "banip", description = "Ban an IP address from the server'.",
        usage = "/<command> <ip> [reason]", aliases = "ban-ip")
public class BanIPCommand extends KoolCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args)
    {
        if (args.length == 0)
        {
            return false;
        }

        if (!InetAddresses.isInetAddress(args[0]))
        {
            msg(sender, "<red>That is not a valid IP address.");
            return true;
        }

        final String ip = args[0].toLowerCase();
        final Ban ban = Ban.builder().by(sender.getName())
                .id(System.currentTimeMillis())
                .expires(System.currentTimeMillis() + BanType.BAN.getLength())
                .ips(new ArrayList<>(List.of(ip)))
                .reason(args.length > 1 ? String.join(" ", ArrayUtils.remove(args, 0)) : null)
                .build();

        boolean success = plugin.banManager.addBan(ban);

        if (!success)
        {
            msg(sender, "<red>That IP address is already permanently banned.");
            return true;
        }

        FUtil.staffAction(sender, "Banned an IP address");
        plugin.recordKeeper.recordPunishment(Punishment.fromBan(ban));

        // Now for the fun part...
        Bukkit.getOnlinePlayers().stream().filter(player -> FUtil.getIp(player).equalsIgnoreCase(ip)).forEach(player ->
        {
            // Now for the fun part...
            for (int i = 0; i < 4; i++)
            {
                player.getWorld().strikeLightning(player.getLocation());
            }
            player.setHealth(0);

            // We had our fun, they're gone
            player.kick(ban.getKickMessage());
        });

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String commandLabel, String[] args)
    {
        return args.length == 1 ? Bukkit.getOnlinePlayers().stream().map(FUtil::getIp).toList() : List.of();
    }
}
