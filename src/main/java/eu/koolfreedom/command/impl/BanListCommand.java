package eu.koolfreedom.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.koolfreedom.banning.Ban;
import eu.koolfreedom.banning.BanManager;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.util.FLog;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;

@CommandParameters(name = "banlist", description = "Manage the ban list.", usage = "/banlist [reload]")
public class BanListCommand extends KoolCommand
{
	private static final int PAGE_SIZE = 10;

	final BanManager banManager = plugin.getBanManager();

	@Override
	public void build(LiteralArgumentBuilder<CommandSourceStack> root)
	{
		root.executes(executes(ctx -> displayPage(sender(ctx), 1)))
				.then(argument("page", IntegerArgumentType.integer(1))
						.executes(executes(ctx -> displayPage(sender(ctx),
								IntegerArgumentType.getInteger(ctx, "page")))))
				.then(literal("reload").executes(executes(ctx ->
				{
					if (!sender(ctx).hasPermission("kfc.senior"))
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
						msg(sender(ctx), "<red>There was an error reloading the banlist");
					}
				})));
	}

	private void displayPage(CommandSender sender, int page)
	{
		var bans = banManager.getActiveBans();
		int totalPages = Math.max(1, (bans.size() + PAGE_SIZE - 1) / PAGE_SIZE);

		if (page > totalPages)
		{
			msg(sender, "<red>There is no banlist page <page>. <gray>The last page is <last>.",
					Placeholder.unparsed("page", String.valueOf(page)),
					Placeholder.unparsed("last", String.valueOf(totalPages)));
			return;
		}

		int start = (page - 1) * PAGE_SIZE;
		int end = Math.min(start + PAGE_SIZE, bans.size());
		boolean canViewIps = sender.hasPermission("kfc.admin");

		msg(sender, "<dark_gray>━━━━━━━━ <red>Active Bans (Page <page>/<pages>)</red> ━━━━━━━━",
				Placeholder.unparsed("page", String.valueOf(page)),
				Placeholder.unparsed("pages", String.valueOf(totalPages)));

		if (bans.isEmpty())
		{
			msg(sender, "<gray>No active bans.");
			return;
		}

		for (Ban ban : bans.subList(start, end))
		{
			String name = ban.getName();
			if (name == null || name.isBlank())
			{
				name = !ban.getIps().isEmpty()
						? (canViewIps ? String.join(", ", ban.getIps()) : "IP address")
						: "Unknown target";
			}

			msg(sender, "<dark_gray>• <gold><name></gold> <gray>- Duration: <yellow><duration></yellow> <gray>- Reason: <white><reason>",
					Placeholder.unparsed("name", name),
					Placeholder.unparsed("duration", ban.getDurationString()),
					Placeholder.unparsed("reason", ban.getReason() != null && !ban.getReason().isBlank()
							? ban.getReason() : "No reason provided"));
		}

		msg(sender, "<dark_gray>━━━━━━━━ <red>Active Bans Count -> <count></red> ━━━━━━━━",
				Placeholder.unparsed("count", String.valueOf(banManager.getBanCount())));
	}
}
