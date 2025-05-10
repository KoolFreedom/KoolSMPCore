package eu.koolfreedom.event;

import eu.koolfreedom.reporting.Report;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class PlayerReportDeleteEvent extends Event
{
	@Getter
	private static final HandlerList handlerList = new HandlerList();

	private final Component staffDisplayName;
	private final String staffName;
	private final String staffId;
	private final List<Report> reports;

	public PlayerReportDeleteEvent(boolean async, Component staffDisplayName, String staffName, String staffId, List<Report> reports)
	{
		super(async);
		this.staffDisplayName = staffDisplayName;
		this.staffName = staffName;
		this.staffId = staffId;
		this.reports = reports;
	}

	@Override
	public @NotNull HandlerList getHandlers()
	{
		return handlerList;
	}
}
