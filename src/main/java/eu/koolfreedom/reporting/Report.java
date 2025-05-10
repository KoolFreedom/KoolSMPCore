package eu.koolfreedom.reporting;

import eu.koolfreedom.config.ConfigEntry;
import eu.koolfreedom.event.PlayerReportEvent;
import eu.koolfreedom.event.PlayerReportUpdateEvent;
import eu.koolfreedom.util.FUtil;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Builder
@Getter
@Setter
public class Report
{
	@Getter
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");

	private String id;
	private final UUID reporter;
	private final UUID reported;
	private final String reason;
	@Builder.Default
	private final long timestamp = System.currentTimeMillis() / 1000;
	@Builder.Default
	private ReportStatus status = ReportStatus.UNRESOLVED;
	@Builder.Default
	private String lastNote = null;
	@Builder.Default
	private List<String> handlers = new ArrayList<>();
	@Builder.Default
	private final ConfigurationSection additionalData = new MemoryConfiguration();

	public static Report fromConfigurationSection(String id, ConfigurationSection section)
	{
		return builder()
				.reporter(UUID.fromString(Objects.requireNonNull(section.getString("reporter"))))
				.reported(UUID.fromString(Objects.requireNonNull(section.getString("reported"))))
				.reason(section.getString("reason"))
				.timestamp(section.getLong("timestamp"))
				.status(ReportStatus.fromNameSafely(section.getString("status")))
				.lastNote(section.getString("lastNote", null))
				.handlers(!section.getStringList("handlers").isEmpty() ?
						new ArrayList<>(section.getStringList("handlers")) :
						section.getString("resolver") != null ?
								new ArrayList<>(List.of(Objects.requireNonNull(section.getString("resolver")))) :
								new ArrayList<>())
				.additionalData(section.isConfigurationSection("extra") ? section.getConfigurationSection("extra") : section.createSection("extra"))
				.id(id)
				.build();
	}

	public static Report forPlayer(Player reporter, OfflinePlayer reported, String reason)
	{
		return builder()
				.reporter(reporter.getUniqueId())
				.reported(reported.getUniqueId())
				.reason(reason)
				.build();
	}

	public ConfigurationSection toConfigurationSection()
	{
		final MemoryConfiguration section = new MemoryConfiguration();
		section.set("reporter", reporter.toString());
		section.set("reported", reported.toString());
		section.set("reason", reason);
		section.set("timestamp", timestamp);
		section.set("status", status.name());
		section.set("lastNote", lastNote);
		section.set("handlers", handlers);
		section.set("extra", additionalData);
		return section;
	}

	public void addHandler(String handlerId)
	{
		handlers.remove(handlerId);
		handlers.addLast(handlerId);
	}

	public boolean update(Component displayName, String staffName, String staffId, ReportStatus newStatus, String note)
	{
		return update(new PlayerReportUpdateEvent(false, displayName, staffName, staffId, getStatus(), newStatus, note, this));
	}

	public boolean updateAsync(Component displayName, String staffName, String staffId, ReportStatus newStatus, String note)
	{
		return update(new PlayerReportUpdateEvent(true, displayName, staffName, staffId, getStatus(), newStatus, note, this));
	}

	private boolean update(PlayerReportUpdateEvent event)
	{
		if (isResolved() && event.getNewStatus() != ReportStatus.REOPENED)
		{
			return false;
		}

		if (event.getNewNote() != null)
		{
			setLastNote(event.getNewNote());
		}

		setStatus(event.getNewStatus());
		addHandler(event.getStaffId());

		event.callEvent();
		return true;
	}

	public PlayerReportEvent createEvent()
	{
		return new PlayerReportEvent(Bukkit.getPlayer(reporter), Bukkit.getOfflinePlayer(reported), reason, this);
	}

	public boolean isResolved()
	{
		return status != ReportStatus.UNRESOLVED && status != ReportStatus.REOPENED;
	}

	public Component summary()
	{
		final OfflinePlayer reporter = Bukkit.getOfflinePlayer(getReporter());
		final OfflinePlayer reported = Bukkit.getOfflinePlayer(getReported());

		return FUtil.miniMessage(ConfigEntry.FORMATS_REPORT_QUICK_SUMMARY.getString(),
				Placeholder.parsed("id", id != null ? id : ""),
				Placeholder.component("reporter_display", reporter instanceof Player player ? player.displayName() : Component.text(reporter.getName() != null ? reporter.getName() : reporter.getUniqueId().toString()).color(NamedTextColor.WHITE)),
				Placeholder.component("reported_display", reported instanceof Player player ? player.displayName() : Component.text(reported.getName() != null ? reported.getName() : reported.getUniqueId().toString()).color(NamedTextColor.WHITE)),
				Placeholder.parsed("reporter", reporter.getName() != null ? reporter.getName() : reporter.getUniqueId().toString()),
				Placeholder.parsed("reported", reported.getName() != null ? reported.getName() : reported.getUniqueId().toString()),
				Placeholder.unparsed("reason", reason),
				Placeholder.parsed("timestamp", String.valueOf(timestamp)),
				Formatter.date("date", LocalDateTime.ofEpochSecond(timestamp, 0, ZoneId.systemDefault().getRules().getOffset(Instant.now()))),
				Placeholder.component("handlers", handlers.isEmpty() ? Component.text("(none)") : Component.join(JoinConfiguration.commas(true),
						handlers.stream().map(handler -> Component.text(handler).color(NamedTextColor.WHITE)).toList())),
				Placeholder.component("status", status.label()),
				Placeholder.unparsed("staff_note", lastNote != null ? lastNote : "(none)"),
				Formatter.booleanChoice("resolved", isResolved()),
				Placeholder.parsed("full_summary", ConfigEntry.FORMATS_REPORT_SUMMARY.getString()));
	}

	public Component fullSummary()
	{
		final OfflinePlayer reporter = Bukkit.getOfflinePlayer(getReporter());
		final OfflinePlayer reported = Bukkit.getOfflinePlayer(getReported());
		return FUtil.miniMessage(ConfigEntry.FORMATS_REPORT_SUMMARY.getString(),
				Placeholder.parsed("id", id != null ? id : ""),
				Placeholder.component("reporter_display", reporter instanceof Player player ? player.displayName() : Component.text(reporter.getName() != null ? reporter.getName() : reporter.getUniqueId().toString()).color(NamedTextColor.WHITE)),
				Placeholder.component("reported_display", reported instanceof Player player ? player.displayName() : Component.text(reported.getName() != null ? reported.getName() : reported.getUniqueId().toString()).color(NamedTextColor.WHITE)),
				Placeholder.parsed("reporter", reporter.getName() != null ? reporter.getName() : reporter.getUniqueId().toString()),
				Placeholder.parsed("reported", reported.getName() != null ? reported.getName() : reported.getUniqueId().toString()),
				Placeholder.unparsed("reason", reason),
				Placeholder.parsed("timestamp", String.valueOf(timestamp)),
				Formatter.date("date", LocalDateTime.ofEpochSecond(timestamp, 0, ZoneId.systemDefault().getRules().getOffset(Instant.now()))),
				Placeholder.component("handlers", handlers.isEmpty() ? Component.text("(none)") : Component.join(JoinConfiguration.commas(true),
						handlers.stream().map(handler -> Component.text(handler).color(NamedTextColor.WHITE)).toList())),
				Placeholder.component("status", status.label()),
				Placeholder.unparsed("staff_note", lastNote != null ? lastNote : "(none)"),
				Formatter.booleanChoice("resolved", isResolved()));
	}

	@Getter
	@RequiredArgsConstructor
	public enum ReportStatus
	{
		RESOLVED("Resolved", Color.GREEN, NamedTextColor.GREEN),
		CLOSED("Closed", Color.GRAY, NamedTextColor.GRAY),
		UNRESOLVED("Unresolved", Color.RED, NamedTextColor.RED),
		REOPENED("Reopened", Color.BLUE, NamedTextColor.BLUE),
		UNKNOWN("Unknown", Color.BLACK, NamedTextColor.DARK_RED);

		private final String label;
		private final Color awtColor;
		private final TextColor inGameColor;

		public Component label()
		{
			return Component.text(label, inGameColor);
		}

		public static ReportStatus fromNameSafely(String name)
		{
			return Arrays.stream(values()).filter(status -> status.name().equalsIgnoreCase(name)).findAny()
					.orElse(ReportStatus.UNKNOWN);
		}
	}
}
