package eu.koolfreedom.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.event.PlayerReportEvent;
import eu.koolfreedom.reporting.Report;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandParameters(name = "report", description = "Report a misbehaving player to staff.",
        usage = "/<command> <player> <reason>")
public class ReportCommand extends KoolCommand
{
    @Override
    public void build(LiteralArgumentBuilder<CommandSourceStack> root)
    {
        root.then(argument("target", StringArgumentType.word())
                .then(argument("reason", StringArgumentType.greedyString())
                        .executes(executes(ctx -> report(sender(ctx), playerSender(ctx),
                                StringArgumentType.getString(ctx, "target"),
                                StringArgumentType.getString(ctx, "reason"))))));
    }

    private void report(CommandSender sender, Player playerSender, String targetName, String reason)
    {
        if (playerSender == null)
        {
            msg(sender, playersOnly);
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.isOnline() && !target.hasPlayedBefore())
        {
            msg(sender, playerNotFound);
            return;
        }

        final PlayerReportEvent event = Report.forPlayer(playerSender, target, reason).createEvent();
        event.callEvent();

        msg(sender, "<green>Thank you. Your report has been logged.");
        msg(sender, "<yellow>Please keep in mind that spamming reports is not allowed, and you will be sanctioned if you do so.");

        if (target.equals(playerSender))
        {
            msg(sender, "<red>But why in the world would you report yourself???");
        }
    }
}
