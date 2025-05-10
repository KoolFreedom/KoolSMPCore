package eu.koolfreedom.reporting;

import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.config.ConfigEntry;
import eu.koolfreedom.event.PlayerReportDeleteEvent;
import eu.koolfreedom.event.PlayerReportEvent;
import eu.koolfreedom.event.PlayerReportUpdateEvent;
import eu.koolfreedom.log.FLog;
import eu.koolfreedom.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ReportManager implements Listener
{
	private static final File reportsFile = new File(KoolSMPCore.getInstance().getDataFolder(), "reports.yml");
	private final Map<String, Report> reportMap = new HashMap<>();

	public ReportManager()
	{
		load();
		Bukkit.getPluginManager().registerEvents(this, KoolSMPCore.getInstance());
	}

	public void load()
	{
		YamlConfiguration configuration = YamlConfiguration.loadConfiguration(reportsFile);
		configuration.getKeys(false).stream().filter(configuration::isConfigurationSection).forEach(id ->
				reportMap.put(id, Report.fromConfigurationSection(id, Objects.requireNonNull(configuration.getConfigurationSection(id)))));
		FLog.info("Reports loaded");
	}

	public void save()
	{
		YamlConfiguration yaml = new YamlConfiguration();
		reportMap.forEach((id, report) -> yaml.set(id, report.toConfigurationSection()));

		try
		{
			yaml.save(reportsFile);
		}
		catch (IOException ex)
		{
			FLog.warning("Failed to save reports data");
		}
	}

	public List<String> getClosedReportIDs()
	{
		return reportMap.entrySet().stream().filter(entry -> entry.getValue().isResolved())
				.map(Map.Entry::getKey).toList();
	}

	public List<String> getReportIds(boolean includeResolved)
	{
		return reportMap.entrySet().stream().filter(entry -> !entry.getValue().isResolved() || includeResolved)
				.map(Map.Entry::getKey).toList();
	}

	public List<Report> getReports(boolean includeResolved)
	{
		return reportMap.values().stream().filter(report -> !report.isResolved() || includeResolved).toList();
	}

	public void deleteReportsByUuidAsync(Component displayName, String staffName, String staffId, UUID uuid)
	{
		new PlayerReportDeleteEvent(true, displayName, staffName, staffId, deleteReportsBy(uuid)).callEvent();
	}

	public void deleteReportsByUuid(Component displayName, String staffName, String staffId, UUID uuid)
	{
		new PlayerReportDeleteEvent(displayName, staffName, staffId, deleteReportsBy(uuid)).callEvent();
	}

	private List<Report> deleteReportsBy(UUID uuid)
	{
		final List<Report> reports = reportMap.values().stream().filter(lol -> lol.getReporter().equals(uuid)).toList();

		reports.forEach(report -> reportMap.remove(report.getId()));

		return reports;
	}

	public Report getReport(String id)
	{
		Report report = reportMap.get(id);

		if (report == null)
		{
			try
			{
				report = reportMap.get(String.valueOf(String.valueOf(Long.parseLong(id)).hashCode()));
			}
			catch (NumberFormatException ignored)
			{
			}
		}

		return report;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onReport(PlayerReportEvent event)
	{
		// Avoid ID collisions
		String id = FUtil.randomString(8);
		while (reportMap.containsKey(id))
		{
			id = FUtil.randomString(8);
		}

		// Add the report to the database
		reportMap.put(id, event.getReport());
		event.getReport().setId(id);

		// Broadcast the report to admins
		FUtil.broadcast("kfc.command.reports", ConfigEntry.FORMATS_REPORT.getString(),
				Placeholder.parsed("reporter", event.getReporter().getName()),
				Placeholder.parsed("player", event.getReported().getName() != null ? event.getReported().getName() : event.getReported().getUniqueId().toString()),
				Placeholder.unparsed("reason", event.getReason()));

		// Send a ping noise to all online admins
		Bukkit.getOnlinePlayers().stream().filter(player -> player.hasPermission("kfc.command.reports"))
				.forEach(player -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F));

		// Save to our database
		CompletableFuture.runAsync(this::save);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onReportResolved(PlayerReportUpdateEvent event)
	{
		final Report report = event.getReport();
		final OfflinePlayer reporter = Bukkit.getOfflinePlayer(report.getReporter());
		final OfflinePlayer reported = Bukkit.getOfflinePlayer(report.getReported());

		// Let the player who filed the report know that it has been marked as resolved
		if (reporter instanceof Player online)
		{
			online.sendRichMessage("<gray>Your report against <white><reported></white> (ID <white><id></white>) has been marked as <status>.",
					Placeholder.unparsed("reported", reported.getName() != null ? reported.getName() : reported.getUniqueId().toString()),
					Placeholder.unparsed("id", report.getId()),
					Placeholder.component("status", event.getNewStatus().label()));
		}

		// Save our database in the background
		CompletableFuture.runAsync(this::save);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void saveOnReportDeletion(PlayerReportDeleteEvent event)
	{
		// Save our database in the background
		CompletableFuture.runAsync(this::save);
	}

	@EventHandler
	public void onAdminJoin(PlayerJoinEvent event)
	{
		final Player player = event.getPlayer();

		if (player.hasPermission("kfc.command.reports"))
		{
			long unresolved = reportMap.values().stream().filter(report -> !report.isResolved()).count();

			if (unresolved > 0)
			{
				player.sendRichMessage("<yellow>⚠ <gray>|</gray> There are <gold><amount></gold> unresolved report(s) requiring your attention. Use <click:run_command:'/reports unresolved'><gold><u>/reports unresolved</u></gold></click> for more information.",
						Placeholder.unparsed("amount", String.valueOf(unresolved)));
			}
		}
	}
}