package eu.koolfreedom.command.impl;

import eu.koolfreedom.api.AltManager;
import eu.koolfreedom.command.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

@CommandParameters(name = "alts", description = "Shows other accounts known to be associated with a player or IP", usage = "/alts <player|ip>")
public class AltsCommand extends KoolCommand
{
    private final AltManager altManager = plugin.getAltManager();

    @Override
    public boolean run(CommandSender sender, org.bukkit.entity.Player player, Command command, String label, String[] args)
    {
        if (args.length != 1)
        {
            msg(sender, "<gray>Usage: /alts <player|ip>");
            return true;
        }

        String input = args[0];
        String ip;

        if (FUtil.isValidIP(input))
        {
            ip = input;
        }
        else
        {
            OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(input);
            if (target == null || (!target.hasPlayedBefore() && !target.isOnline()))
            {
                msg(sender, playerNotFound);
                return true;
            }

            ip = altManager.getLastIP(target.getUniqueId()).orElse(null);
            if (ip == null)
            {
                msg(sender, "<red>No IP history found for <white>" + target.getName());
                return true;
            }
        }

        List<OfflinePlayer> matches = FUtil.getOfflinePlayersByIp(ip);
        if (matches.isEmpty())
        {
            msg(sender, "<red>No other accounts found for IP <white>" + ip);
            return true;
        }

        msg(sender, "<red>Accounts linked to IP <white>" + ip + "</white>:");

        for (OfflinePlayer p : matches)
        {
            UUID uuid = p.getUniqueId();
            String name = p.getName() != null ? p.getName() : uuid.toString();
            boolean banned = plugin.getBanManager().isBanned(p);
            boolean flagged = plugin.getNoteManager().hasNotes(uuid);

            TextComponent.Builder line = Component.text().append(Component.text("- ", NamedTextColor.GRAY));
            line.append(Component.text(name, NamedTextColor.YELLOW));

            if (banned)
                line.append(Component.text(" [BANNED]", NamedTextColor.RED, TextDecoration.BOLD));
            if (flagged)
                line.append(Component.text(" [FLAGGED]", NamedTextColor.DARK_PURPLE));

            msg(sender, line.build());
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
