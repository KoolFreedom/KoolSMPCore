package eu.koolfreedom.command.impl;

import com.google.common.net.InetAddresses;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.koolfreedom.banning.Ban;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.punishment.Punishment;
import eu.koolfreedom.util.FUtil;
import eu.koolfreedom.util.TimeOffset;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

@CommandParameters(name = "banip", description = "Ban an IP address from the server'.",
        usage = "/<command> <ip> [reason]", aliases = "ban-ip")
public class BanIPCommand extends KoolCommand
{
    @Override
    public void build(LiteralArgumentBuilder<CommandSourceStack> root)
    {
        root.then(argument("ip", StringArgumentType.word())
                .executes(executes(ctx -> banIp(sender(ctx), StringArgumentType.getString(ctx, "ip"), null)))
                .then(argument("reason", StringArgumentType.greedyString())
                        .executes(executes(ctx -> banIp(sender(ctx), StringArgumentType.getString(ctx, "ip"),
                                StringArgumentType.getString(ctx, "reason"))))));
    }

    private void banIp(CommandSender sender, String ipArg, String reason)
    {
        if (!InetAddresses.isInetAddress(ipArg))
        {
            msg(sender, "<red>That is not a valid IP address.");
            return;
        }

        final String ip = ipArg.toLowerCase();
        final Ban ban = Ban.builder().by(sender.getName())
                .id(System.currentTimeMillis())
                .expires(System.currentTimeMillis() + TimeOffset.getOffset("1d"))
                .ips(new ArrayList<>(List.of(ip)))
                .reason(reason)
                .build();

        boolean success = plugin.getBanManager().addBan(ban);

        if (!success)
        {
            msg(sender, "<red>That IP address is already permanently banned.");
            return;
        }

        FUtil.staffAction(sender, "Banned an IP address");
        plugin.getRecordKeeper().recordPunishment(Punishment.fromBan(ban));

        // Now for the fun part...
        Bukkit.getOnlinePlayers().stream().filter(player -> FUtil.getIp(player).equalsIgnoreCase(ip)).forEach(player ->
        {
            for (int i = 0; i < 4; i++)
            {
                player.getWorld().strikeLightning(player.getLocation());
            }
            player.setHealth(0);

            // We had our fun, they're gone
            player.kick(ban.getKickMessage());
        });
    }
}
