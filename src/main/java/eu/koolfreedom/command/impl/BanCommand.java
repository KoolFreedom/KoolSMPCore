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

@CommandParameters(name = "ban", description = "Ban someone from the server.", usage = "/<command> <player> [reason]",
        aliases = "gtfo")
public class BanCommand extends KoolCommand
{
    @Override
    public void build(LiteralArgumentBuilder<CommandSourceStack> root)
    {
        root.then(argument("target", StringArgumentType.word())
                .executes(executes(ctx -> ban(sender(ctx), StringArgumentType.getString(ctx, "target"), null)))
                .then(argument("reason", StringArgumentType.greedyString())
                        .executes(executes(ctx -> ban(sender(ctx), StringArgumentType.getString(ctx, "target"),
                                StringArgumentType.getString(ctx, "reason"))))));
    }

    private void ban(CommandSender sender, String targetName, String reason)
    {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        final Ban ban;
        if (!target.isOnline() && !target.hasPlayedBefore())
        {
            // Pre-emptive ban — player has never joined, ban by username only
            ban = Ban.fromUsername(targetName, sender.getName(), reason, TimeOffset.getOffset("1d"));
        }
        else
        {
            ban = Ban.fromPlayer(target, sender.getName(), reason, TimeOffset.getOffset("1d"));
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
