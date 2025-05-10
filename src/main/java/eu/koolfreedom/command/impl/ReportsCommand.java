package eu.koolfreedom.command.impl;

import com.google.common.collect.Lists;
import eu.koolfreedom.command.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.config.ConfigEntry;
import eu.koolfreedom.reporting.Report;
import eu.koolfreedom.reporting.ReportManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;

@CommandParameters(name = "reports", description = "Manage player reports.",
		usage = "/reports <list | unresolved | <summary> <id> | purge <player> | <close | handle | reopen> <id> <reason>>")
public class ReportsCommand extends KoolCommand
{
	@Override
	public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args)
	{
		if (args.length == 0)
		{
			return false;
		}

		final ReportManager reportManager = plugin.reportManager;

		switch (args[0].toLowerCase())
		{
			// Surface level, does not require a report ID to be specified
			case "list" ->
			{
				int perPage = args.length >= 2 && args[args.length - 1].equalsIgnoreCase("more") ? 18 : 8;
				final List<List<Report>> paginated = Lists.partition(reportManager.getReports(true).stream()
						.sorted(Comparator.comparingLong(Report::getTimestamp).reversed()).toList(), perPage);

				if (paginated.isEmpty())
				{
					msg(sender, "<green>There are no reports.");
					return true;
				}

				int page = 0;

				try
				{
					page = args.length >= 2 ? Math.min(Math.max(Integer.parseInt(args[1]) - 1, 0), paginated.size() - 1) : 0;
				}
				catch (NumberFormatException ignored)
				{
				}

				final List<Report> results = paginated.get(page);

				msg(sender, "<gradient:dark_gray:gray>████</gradient> Recent Reports <gradient:gray:dark_gray>████</gradient>");
				results.forEach(report -> msg(sender, report.summary()));
				for (int i = 0; i < Math.max((perPage - results.size()), 0); i++)
				{
					msg(sender, ConfigEntry.FORMATS_REPORT_EMPTY_QUICK_SUMMARY.getString());
				}

				msg(sender, "<gradient:dark_gray:gray>████</gradient> Page <page> of <pages> <gradient:gray:dark_gray>████</gradient>",
						Placeholder.unparsed("page", String.valueOf(page + 1)),
						Placeholder.unparsed("pages", String.valueOf(paginated.size())));
			}
			case "unresolved" ->
			{
				int perPage = args.length >= 2 && args[args.length - 1].equalsIgnoreCase("more") ? 18 : 8;
				final List<List<Report>> paginated = Lists.partition(reportManager.getReports(false).stream()
						.sorted(Comparator.comparingLong(Report::getTimestamp).reversed()).toList(), perPage);

				if (paginated.isEmpty())
				{
					msg(sender, "<green>There are no reports for you to handle.");
					return true;
				}

				int page = 0;

				try
				{
					page = args.length >= 2 ? Math.min(Math.max(Integer.parseInt(args[1]) - 1, 0), paginated.size() - 1) : 0;
				}
				catch (NumberFormatException ignored)
				{
				}

				final List<Report> results = paginated.get(page);

				msg(sender, "<gradient:dark_gray:gray>████</gradient> Unhandled Reports <gradient:gray:dark_gray>████</gradient>");
				results.forEach(report -> msg(sender, report.summary()));
				for (int i = 0; i < Math.max((perPage - results.size()), 0); i++)
				{
					msg(sender, ConfigEntry.FORMATS_REPORT_EMPTY_QUICK_SUMMARY.getString());
				}

				msg(sender, "<gradient:dark_gray:gray>████</gradient> Page <page> of <pages> <gradient:gray:dark_gray>████</gradient>",
						Placeholder.unparsed("page", String.valueOf(page + 1)),
						Placeholder.unparsed("pages", String.valueOf(paginated.size())));
			}

			// Requires additional parameters
			case "summary" ->
			{
				if (args.length != 2) return false;

				final Report workingReport = reportManager.getReport(args[1]);

				if (workingReport == null)
				{
					msg(sender, "<red>No report could be found with that ID.");
					return true;
				}

				msg(sender, workingReport.fullSummary());
			}
			case "reopen" ->
			{
				if (args.length < 2) return false;
				final Report report = reportManager.getReport(args[1]);
				final String reason = args.length >= 3 ? StringUtils.join(ArrayUtils.subarray(args, 2, args.length), " ") : "Pending further investigation";

				if (report == null)
				{
					msg(sender, "<red>No report could be found with that ID.");
					return true;
				}
				else if (!report.isResolved())
				{
					msg(sender, "<red>That report is already open.");
					return true;
				}

				report.update(playerSender != null ? playerSender.displayName() : Component.text(sender.getName()),
						sender.getName(),
						playerSender != null ? playerSender.getUniqueId().toString() : sender.getName(),
						Report.ReportStatus.REOPENED,
						reason);

				msg(sender, "<green>Report <dark_green><id></dark_green> has been re-opened.",
						Placeholder.unparsed("id", report.getId() != null ? report.getId() : ""));
			}
			case "handle" ->
			{
				if (args.length == 1) return false;
				final Report report = reportManager.getReport(args[1]);
				final String reason = args.length >= 3 ? StringUtils.join(ArrayUtils.subarray(args, 2, args.length), " ") : "Handled";

				if (report == null)
				{
					msg(sender, "<red>No report could be found with that ID.");
					return true;
				}
				else if (report.isResolved())
				{
					msg(sender, "<red>That report is already closed.");
					return true;
				}

				report.update(playerSender != null ? playerSender.displayName() : Component.text(sender.getName()),
						sender.getName(),
						playerSender != null ? playerSender.getUniqueId().toString() : sender.getName(),
						Report.ReportStatus.CLOSED,
						reason);

				msg(sender, "<green>Report <dark_green><id></dark_green> has been handled.",
						Placeholder.unparsed("id", report.getId() != null ? report.getId() : ""));
			}
			case "close" ->
			{
				if (args.length < 2) return false;
				final Report report = reportManager.getReport(args[1]);
				final String reason = args.length >= 3 ? StringUtils.join(ArrayUtils.subarray(args, 2, args.length), " ") : "Invalid";

				if (report == null)
				{
					msg(sender, "<red>No report could be found with that ID.");
					return true;
				}
				else if (report.isResolved())
				{
					msg(sender, "<red>That report is already closed.");
					return true;
				}

				report.update(playerSender != null ? playerSender.displayName() : Component.text(sender.getName()),
						sender.getName(),
						playerSender != null ? playerSender.getUniqueId().toString() : sender.getName(),
						Report.ReportStatus.CLOSED,
						reason);

				msg(sender, "<green>Report <dark_green><id></dark_green> has been closed.",
						Placeholder.unparsed("id", report.getId() != null ? report.getId() : ""));
			}
			case "purge" ->
			{
				if (!sender.hasPermission("kfc.command.reports.purge"))
				{
					msg(sender, "<red>You don't have permission to purge reports.");
					return true;
				}

				if (args.length == 1) return false;

				OfflinePlayer player = Bukkit.getOfflinePlayer(args[1]);
				if (!player.isOnline() && !player.hasPlayedBefore())
				{
					msg(sender, playerNotFound);
					return true;
				}

				reportManager.deleteReportsByUuid(playerSender != null ? playerSender.displayName() : Component.text(sender.getName()),
						sender.getName(),
						playerSender != null ? playerSender.getUniqueId().toString() : sender.getName(),
						player.getUniqueId());

				msg(sender, "<green>All reports filed by this user have been purged.");

				if (plugin.discord.isEnabled() && !ConfigEntry.DISCORD_REPORTS_CHANNEL_ID.getString().isBlank())
				{
					msg(sender, "<yellow>Please keep in mind that depending on the number of reports, it may take a while for reports to be removed from Discord.");
				}
			}
			default ->
			{
				return false;
			}
		}

		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, Command command, String commandLabel, String[] args)
	{
		final List<String> acceptableModes = List.of("summary", "close", "handle", "reopen", "list", "unresolved", "purge");

		final ReportManager reportManager = plugin.reportManager;

		// Show the available modes
		if (args.length == 1)
		{
			return acceptableModes;
		}
		// Tab complete for modes
		else if (acceptableModes.contains(args[0].toLowerCase()))
		{
			// Tab complete, but also give the summary of the report before the user executes it (if present)
			if (args.length == 2)
			{
				// "recent" and "unresolved" should only show an empty list
				if (!args[0].equalsIgnoreCase("list") && !args[0].equalsIgnoreCase("unresolved")
						&& !args[0].equalsIgnoreCase("purge"))
				{
					// Don't show the summary of the report unless the user intends to close, handle, or re-open a report
					boolean dontShowSummary = args[0].equalsIgnoreCase("summary");
					boolean reopeningReports = args[0].equalsIgnoreCase("reopen");

					if (!dontShowSummary)
					{
						Report report = reportManager.getReport(args[1]);

						if (report != null && (!report.isResolved() || reopeningReports))
						{
							msg(sender, report.fullSummary());
						}
					}

					return args[0].equalsIgnoreCase("reopen") ? reportManager.getClosedReportIDs()
							: reportManager.getReportIds(dontShowSummary);
				}
				else if (args[0].equalsIgnoreCase("purge") && sender.hasPermission("kfc.command.reports.purge"))
				{
					return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
				}
			}
			// Not for tab completions but shows the user the summary of the report before they execute it
			else if (args.length == 3)
			{
				if (args[0].equalsIgnoreCase("close") || args[0].equalsIgnoreCase("handle"))
				{
					Report report = reportManager.getReport(args[1]);

					if (report != null && !report.isResolved() && args[2].isBlank())
					{
						msg(sender, report.fullSummary());
					}
				}
			}
		}

		return List.of();
	}
}
