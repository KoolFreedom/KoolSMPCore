package eu.koolfreedom.event;

import eu.koolfreedom.reporting.Report;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
@RequiredArgsConstructor
public class PlayerReportEvent extends Event
{
	@Getter
	private static final HandlerList handlerList = new HandlerList();

	private final Player reporter;
	private final OfflinePlayer reported;
	private final String reason;
	private final Report report;

	@Override
	public @NotNull HandlerList getHandlers()
	{
		return handlerList;
	}
}
