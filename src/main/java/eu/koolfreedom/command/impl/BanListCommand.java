package eu.koolfreedom.command.impl;

import eu.koolfreedom.banning.BanManager;
import eu.koolfreedom.command.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@CommandParameters(name = "banlist", description = "Manage the ban list.", usage = "/banlist [reload]")
public class BanListCommand extends KoolCommand
{
	@Override
	public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args)
	{
		BanManager banManager = plugin.banManager;

		if (args.length == 0 || !sender.hasPermission("kfc.command.banlist.reload"))
		{
			msg(sender, "<gray>There are currently <amount> entries in the ban list.",
					Placeholder.unparsed("amount", String.valueOf(banManager.getBanCount())));
			return true;
		}

		if (args[0].equalsIgnoreCase("reload"))
		{
			banManager.load();
			msg(sender, "<green>The ban list was reloaded.");
			return true;
		}

		return false;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, Command command, String commandLabel, String[] args)
	{
		return args.length == 1 && sender.hasPermission("kfc.command.banlist.reload") ? List.of("reload") : null;
	}
}
