package eu.koolfreedom.command.impl;

import com.google.common.collect.Lists;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.config.ConfigEntry;
import eu.koolfreedom.reporting.Report;
import eu.koolfreedom.reporting.ReportManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;

@CommandParameters(name = "reports", description = "Manage player reports.",
		usage = "/reports <list | unresolved | <summary> <id> | purge <player> | <close | handle | reopen> <id> <reason>>")
public class ReportsCommand extends KoolCommand
{
	@Override
	public void build(LiteralArgumentBuilder<CommandSourceStack> root)
	{
		root.then(literal("list")
						.executes(executes(ctx -> list(sender(ctx), true, 1, 8)))
						.then(literal("more").executes(executes(ctx -> list(sender(ctx), true, 1, 18))))
						.then(argument("page", IntegerArgumentType.integer(1))
								.executes(executes(ctx -> list(sender(ctx), true, IntegerArgumentType.getInteger(ctx, "page"), 8)))
								.then(literal("more").executes(executes(ctx -> list(sender(ctx), true,
										IntegerArgumentType.getInteger(ctx, "page"), 18))))))
				.then(literal("unresolved")
						.executes(executes(ctx -> list(sender(ctx), false, 1, 8)))
						.then(literal("more").executes(executes(ctx -> list(sender(ctx), false, 1, 18))))
						.then(argument("page", IntegerArgumentType.integer(1))
								.executes(executes(ctx -> list(sender(ctx), false, IntegerArgumentType.getInteger(ctx, "page"), 8)))
								.then(literal("more").executes(executes(ctx -> list(sender(ctx), false,
										IntegerArgumentType.getInteger(ctx, "page"), 18))))))
				.then(literal("summary").then(argument("id", StringArgumentType.word())
						.executes(executes(ctx -> summary(sender(ctx), StringArgumentType.getString(ctx, "id"))))))
				.then(literal("reopen").then(argument("id", StringArgumentType.word())
						.executes(executes(ctx -> reopen(sender(ctx), playerSender(ctx), StringArgumentType.getString(ctx, "id"), null)))
						.then(argument("reason", StringArgumentType.greedyString())
								.executes(executes(ctx -> reopen(sender(ctx), playerSender(ctx), StringArgumentType.getString(ctx, "id"),
										StringArgumentType.getString(ctx, "reason")))))))
				.then(literal("handle").then(argument("id", StringArgumentType.word())
						.executes(executes(ctx -> handle(sender(ctx), playerSender(ctx), StringArgumentType.getString(ctx, "id"), null)))
						.then(argument("reason", StringArgumentType.greedyString())
								.executes(executes(ctx -> handle(sender(ctx), playerSender(ctx), StringArgumentType.getString(ctx, "id"),
										StringArgumentType.getString(ctx, "reason")))))))
				.then(literal("close").then(argument("id", StringArgumentType.word())
						.executes(executes(ctx -> close(sender(ctx), playerSender(ctx), StringArgumentType.getString(ctx, "id"), null)))
						.then(argument("reason", StringArgumentType.greedyString())
								.executes(executes(ctx -> close(sender(ctx), playerSender(ctx), StringArgumentType.getString(ctx, "id"),
										StringArgumentType.getString(ctx, "reason")))))))
				.then(literal("purge").then(argument("player", StringArgumentType.word())
						.executes(executes(ctx -> purge(sender(ctx), playerSender(ctx), StringArgumentType.getString(ctx, "player"))))));
	}

	private void list(CommandSender sender, boolean resolved, int page, int perPage)
	{
		final ReportManager reportManager = plugin.getReportManager();
		final List<List<Report>> paginated = Lists.partition(reportManager.getReports(resolved).stream()
				.sorted(Comparator.comparingLong(Report::getTimestamp).reversed()).toList(), perPage);

		String emptyMessage = resolved ? "<green>There are no reports." : "<green>There are no reports for you to handle.";
		if (paginated.isEmpty())
		{
			msg(sender, emptyMessage);
			return;
		}

		int index = Math.clamp(page - 1, 0, paginated.size() - 1);
		final List<Report> results = paginated.get(index);

		String header = resolved ? "Recent Reports" : "Unhandled Reports";
		msg(sender, "<gradient:dark_gray:gray>████</gradient> " + header + " <gradient:gray:dark_gray>████</gradient>");
		results.forEach(report -> msg(sender, report.summary()));
		for (int i = 0; i < Math.max((perPage - results.size()), 0); i++)
		{
			msg(sender, ConfigEntry.FORMATS_REPORT_EMPTY_QUICK_SUMMARY.getString());
		}

		msg(sender, "<gradient:dark_gray:gray>████</gradient> Page <page> of <pages> <gradient:gray:dark_gray>████</gradient>",
				Placeholder.unparsed("page", String.valueOf(index + 1)),
				Placeholder.unparsed("pages", String.valueOf(paginated.size())));
	}

	private void summary(CommandSender sender, String id)
	{
		final Report report = plugin.getReportManager().getReport(id);

		if (report == null)
		{
			msg(sender, "<red>No report could be found with that ID.");
			return;
		}

		msg(sender, report.fullSummary());
	}

	private void reopen(CommandSender sender, Player playerSender, String id, String reasonArg)
	{
		if (!sender.hasPermission("kfc.reports.reopen"))
		{
			msg(sender, "<red>You don't have permission to reopen reports.");
			return;
		}

		final Report report = plugin.getReportManager().getReport(id);
		final String reason = reasonArg != null ? reasonArg : "Pending further investigation";

		if (report == null)
		{
			msg(sender, "<red>No report could be found with that ID.");
			return;
		}
		else if (!report.isResolved())
		{
			msg(sender, "<red>That report is already open.");
			return;
		}

		report.update(playerSender != null ? playerSender.displayName() : Component.text(sender.getName()),
				sender.getName(),
				playerSender != null ? playerSender.getUniqueId().toString() : sender.getName(),
				Report.ReportStatus.REOPENED,
				reason);

		msg(sender, "<green>Report <dark_green><id></dark_green> has been re-opened.",
				Placeholder.unparsed("id", report.getId() != null ? report.getId() : ""));
	}

	private void handle(CommandSender sender, Player playerSender, String id, String reasonArg)
	{
		if (!sender.hasPermission("kfc.reports.handle"))
		{
			msg(sender, "<red>You don't have permission to handle reports.");
			return;
		}

		final Report report = plugin.getReportManager().getReport(id);
		final String reason = reasonArg != null ? reasonArg : "Handled";

		if (report == null)
		{
			msg(sender, "<red>No report could be found with that ID.");
			return;
		}
		else if (report.isResolved())
		{
			msg(sender, "<red>That report is already closed.");
			return;
		}

		report.update(playerSender != null ? playerSender.displayName() : Component.text(sender.getName()),
				sender.getName(),
				playerSender != null ? playerSender.getUniqueId().toString() : sender.getName(),
				Report.ReportStatus.CLOSED,
				reason);

		msg(sender, "<green>Report <dark_green><id></dark_green> has been handled.",
				Placeholder.unparsed("id", report.getId() != null ? report.getId() : ""));
	}

	private void close(CommandSender sender, Player playerSender, String id, String reasonArg)
	{
		if (!sender.hasPermission("kfc.reports.close"))
		{
			msg(sender, "<red>You don't have permission to close reports.");
			return;
		}

		final Report report = plugin.getReportManager().getReport(id);
		final String reason = reasonArg != null ? reasonArg : "Invalid";

		if (report == null)
		{
			msg(sender, "<red>No report could be found with that ID.");
			return;
		}
		else if (report.isResolved())
		{
			msg(sender, "<red>That report is already closed.");
			return;
		}

		report.update(playerSender != null ? playerSender.displayName() : Component.text(sender.getName()),
				sender.getName(),
				playerSender != null ? playerSender.getUniqueId().toString() : sender.getName(),
				Report.ReportStatus.CLOSED,
				reason);

		msg(sender, "<green>Report <dark_green><id></dark_green> has been closed.",
				Placeholder.unparsed("id", report.getId() != null ? report.getId() : ""));
	}

	private void purge(CommandSender sender, Player playerSender, String playerName)
	{
		if (!sender.hasPermission("kfc.reports.purge"))
		{
			msg(sender, "<red>You don't have permission to purge reports.");
			return;
		}

		OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
		if (!player.isOnline() && !player.hasPlayedBefore())
		{
			msg(sender, playerNotFound);
			return;
		}

		plugin.getReportManager().deleteReportsByUuid(playerSender != null ? playerSender.displayName() : Component.text(sender.getName()),
				sender.getName(),
				playerSender != null ? playerSender.getUniqueId().toString() : sender.getName(),
				player.getUniqueId());

		msg(sender, "<green>All reports filed by this user have been purged.");

		if (plugin.getDiscordBridge() != null && plugin.getDiscordBridge().channelExists("reports"))
		{
			msg(sender, "<yellow>Please keep in mind that depending on the number of reports, it may take a while for reports to be removed from Discord.");
		}
	}
}
