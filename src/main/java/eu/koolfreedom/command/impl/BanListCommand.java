package eu.koolfreedom.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.koolfreedom.banning.BanManager;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.util.FLog;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@CommandParameters(name = "banlist", description = "Manage the ban list.", usage = "/banlist [reload]")
public class BanListCommand extends KoolCommand
{
	final BanManager banManager = plugin.getBanManager();

	@Override
	public void build(LiteralArgumentBuilder<CommandSourceStack> root)
	{
		root.executes(executes(ctx ->
				{
					msg(sender(ctx), "<gray>There are <count> indefinite bans.",
							Placeholder.unparsed("count", String.valueOf(banManager.getBanCount())));
				}))
				.then(literal("reload").executes(executes(ctx ->
				{
					if (!sender(ctx).hasPermission("kfc.banlist.reload"))
					{
						msg(sender(ctx), noPermission);
						return;
					}

					try
					{
						banManager.load();
						msg(sender(ctx), "<gray>Reloaded the banlist");
					}
					catch (Exception ex)
					{
						FLog.error("Failed to reload banlist", ex);
						msg(sender(ctx), "<red>There was an error reloading the banliste");
					}
				})));
	}
}
