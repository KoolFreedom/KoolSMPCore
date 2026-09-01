package eu.koolfreedom.reporting;

import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.config.ConfigEntry;
import eu.koolfreedom.event.PlayerReportDeleteEvent;
import eu.koolfreedom.event.PlayerReportEvent;
import eu.koolfreedom.event.PlayerReportUpdateEvent;
import eu.koolfreedom.listener.KoolListener;
import eu.koolfreedom.util.FLog;
import eu.koolfreedom.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ReportManager extends KoolListener
{
	private final Map<String, Report> reportMap;

	public ReportManager()
	{
		reportMap = new ConcurrentHashMap<>(KoolSMPCore.getInstance().getPlayerRegistry().loadAllReports());
		FLog.info("{} report(s) loaded.", reportMap.size());
	}

	// --- Queries ---

	public List<String> getClosedReportIDs()
	{
		return reportMap.entrySet().stream()
				.filter(e -> e.getValue().isResolved())
				.map(Map.Entry::getKey).toList();
	}

	public List<String> getReportIds(boolean includeResolved)
	{
		return reportMap.entrySet().stream()
				.filter(e -> !e.getValue().isResolved() || includeResolved)
				.map(Map.Entry::getKey).toList();
	}

	public List<Report> getReports(boolean includeResolved)
	{
		return reportMap.values().stream()
				.filter(r -> !r.isResolved() || includeResolved).toList();
	}

	public Report getReport(String id)
	{
		Report report = reportMap.get(id);
		if (report == null)
		{
			try { report = reportMap.get(String.valueOf(String.valueOf(Long.parseLong(id)).hashCode())); }
			catch (NumberFormatException ignored) {}
		}
		return report;
	}

	// --- Mutations ---

	public void deleteReportsByUuid(Component displayName, String staffName, String staffId, UUID uuid)
	{
		new PlayerReportDeleteEvent(false, displayName, staffName, staffId, deleteReportsBy(uuid)).callEvent();
	}

	public void deleteReportsByUuidAsync(Component displayName, String staffName, String staffId, UUID uuid)
	{
		new PlayerReportDeleteEvent(true, displayName, staffName, staffId, deleteReportsBy(uuid)).callEvent();
	}

	private List<Report> deleteReportsBy(UUID uuid)
	{
		List<Report> reports = reportMap.values().stream()
				.filter(r -> r.getReporter().equals(uuid)).toList();
		reports.forEach(r ->
		{
			reportMap.remove(r.getId());
			playerRegistry.deleteReport(r.getId());
		});
		return reports;
	}

	// --- Events ---

	@EventHandler(priority = EventPriority.LOWEST)
	public void onReport(PlayerReportEvent event)
	{
		// Avoid ID collisions
		String id = FUtil.randomString(8);
		while (reportMap.containsKey(id)) id = FUtil.randomString(8);

		Report report = event.getReport();
		report.setId(id);
		reportMap.put(id, report);
		playerRegistry.saveReport(report);

		FUtil.broadcast("kfc.command.reports", ConfigEntry.FORMATS_REPORT.getString(),
				Placeholder.parsed("reporter", event.getReporter().getName()),
				Placeholder.parsed("player", event.getReported().getName() != null
						? event.getReported().getName()
						: event.getReported().getUniqueId().toString()),
				Placeholder.unparsed("reason", event.getReason()));

		Bukkit.getOnlinePlayers().stream()
				.filter(p -> p.hasPermission("kfc.command.reports"))
				.forEach(p -> p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F));
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onReportResolved(PlayerReportUpdateEvent event)
	{
		Report report = event.getReport();
		playerRegistry.saveReport(report);

		OfflinePlayer reporter = Bukkit.getOfflinePlayer(report.getReporter());
		OfflinePlayer reported = Bukkit.getOfflinePlayer(report.getReported());

		if (reporter instanceof Player online)
		{
			online.sendRichMessage(
					"<gray>Your report against <white><reported></white> (ID <white><id></white>) has been marked as <status>.",
					Placeholder.unparsed("reported", reported.getName() != null
							? reported.getName() : reported.getUniqueId().toString()),
					Placeholder.unparsed("id", report.getId()),
					Placeholder.component("status", event.getNewStatus().label()));
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onReportDeleted(PlayerReportDeleteEvent event)
	{
		// Already removed from DB in deleteReportsBy(); nothing extra needed here.
	}

	@EventHandler
	public void onAdminJoin(PlayerJoinEvent event)
	{
		Player player = event.getPlayer();
		if (!player.hasPermission("kfc.command.reports")) return;

		long unresolved = reportMap.values().stream().filter(r -> !r.isResolved()).count();
		if (unresolved > 0)
		{
			player.sendRichMessage(
					"<yellow>⚠ <gray>|</gray> There are <gold><amount></gold> unresolved report(s). "
							+ "Use <click:run_command:'/reports unresolved'><gold><u>/reports unresolved</u></gold></click>.",
					Placeholder.unparsed("amount", String.valueOf(unresolved)));
		}
	}
}