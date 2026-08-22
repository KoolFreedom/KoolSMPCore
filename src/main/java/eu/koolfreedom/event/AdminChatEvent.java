package eu.koolfreedom.event;

import eu.koolfreedom.bridge.GroupManagement;
import lombok.Getter;
import lombok.NonNull;
import net.kyori.adventure.key.Namespaced;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@Getter
public class AdminChatEvent extends Event
{
	@Getter
	private static final HandlerList handlerList = new HandlerList();

	private final @Nullable CommandSender commandSender;
	private final @Nullable Component senderDisplay;
	private final @Nullable String senderName;
	private final @NonNull GroupManagement.Group senderGroup;
	private final @NonNull Component message;
	private final @NonNull Namespaced source;

	public AdminChatEvent(boolean async, @Nullable CommandSender commandSender, @Nullable Component senderDisplay,
						  @Nullable String senderName, @NonNull GroupManagement.Group senderGroup,
						  @NonNull Component message, @NonNull Namespaced source)
	{
		super(async);

		if (commandSender == null)
		{
			Objects.requireNonNull(senderName);
			Objects.requireNonNull(senderDisplay);
		}
		else if (senderName == null && senderDisplay == null)
		{
			Objects.requireNonNull(commandSender);
		}

		this.commandSender = commandSender;
		this.senderDisplay = senderDisplay;
		this.senderName = senderName;
		this.senderGroup = senderGroup;
		this.message = message;
		this.source = source;
	}

	@Override
	public @NotNull HandlerList getHandlers()
	{
		return handlerList;
	}
}
