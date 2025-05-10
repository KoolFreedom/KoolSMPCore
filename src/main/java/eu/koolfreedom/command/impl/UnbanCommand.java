package eu.koolfreedom.command.impl;

import eu.koolfreedom.banning.Ban;
import eu.koolfreedom.command.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.util.FUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandParameters(name = "unban", description = "Unban a player or IP address.", usage = "/<command> <playerOrIp>",
        aliases = {"pardon", "pardon-ip", "unbanip"})
public class UnbanCommand extends KoolCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args)
    {
        if (args.length != 1)
        {
            return false;
        }

        Ban ban = plugin.banManager.removeBan(args[0]);

        if (ban == null)
        {
            msg(sender, "<red>An entry could not be found which fit the criteria.");
        }
        else if (!ban.canExpire())
        {
            msg(sender, "<red>Permanent bans cannot be removed from in-game for security reasons.");
        }
        else
        {
            String name = ban.getName() != null ? ban.getName() : ban.getUuid() != null ? Bukkit.getOfflinePlayer(ban.getUuid()).getName() : null;

            if (name != null)
            {
                FUtil.staffAction(sender, "Unbanned <player>", Placeholder.unparsed("player", name));
            }
            else
            {
                FUtil.staffAction(sender, "Unbanned an IP address");
            }
        }

        return true;
    }
}
