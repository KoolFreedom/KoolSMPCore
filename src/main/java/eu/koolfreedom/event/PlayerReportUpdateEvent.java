package eu.koolfreedom.event;

import eu.koolfreedom.reporting.Report;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
@RequiredArgsConstructor
public class PlayerReportUpdateEvent extends Event
{
	@Getter
	private static final HandlerList handlerList = new HandlerList();

	private final Component staffDisplayName;
	private final String staffName;
	private final String staffId;
	private final Report.ReportStatus oldStatus;
	private final Report.ReportStatus newStatus;
	private final String newNote;
	private final Report report;

	public PlayerReportUpdateEvent(boolean async, Component staffDisplayName, String staffName, String staffId, Report.ReportStatus oldStatus, Report.ReportStatus newStatus, String newNote, Report report)
	{
		super(async);
		this.staffDisplayName = staffDisplayName;
		this.staffName = staffName;
		this.staffId = staffId;
		this.oldStatus = oldStatus;
		this.newStatus = newStatus;
		this.newNote = newNote;
		this.report = report;
	}

	@Override
	public @NotNull HandlerList getHandlers()
	{
		return handlerList;
	}
}
