package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.CommandParameters;
import eu.koolfreedom.command.KoolCommand;

import java.util.List;

import eu.koolfreedom.event.PlayerReportEvent;
import eu.koolfreedom.reporting.Report;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandParameters(name = "report", description = "Report a misbehaving player to staff.",
        aliases = {"holyshitfuckinghelp"}, usage = "/<command> <player> <reason>")
public class ReportCommand extends KoolCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args)
    {
        if (playerSender == null)
        {
            msg(sender, playersOnly);
            return true;
        }

        if (args.length <= 1)
        {
            return false;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.isOnline() && !target.hasPlayedBefore())
        {
            msg(sender, playerNotFound);
            return true;
        }

        String reason = String.join(" ", ArrayUtils.remove(args, 0));

        final PlayerReportEvent event = Report.forPlayer(playerSender, target, reason).createEvent();
        event.callEvent();

        msg(sender, "<green>Thank you. Your report has been logged.");
        msg(sender, "<yellow>Please keep in mind that spamming reports is not allowed, and you will be sanctioned if you do so.");

        if (target.equals(playerSender))
        {
            msg(sender, "<red>But why in the world would you report yourself???");
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String commandLabel, String[] args)
    {
        return args.length == 1 ? Bukkit.getOnlinePlayers().stream().map(Player::getName).toList() : List.of();
    }
}