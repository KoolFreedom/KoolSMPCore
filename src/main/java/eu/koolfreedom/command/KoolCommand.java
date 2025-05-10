package eu.koolfreedom.command;

import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.log.FLog;
import eu.koolfreedom.util.FUtil;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public abstract class KoolCommand extends Command implements PluginIdentifiableCommand
{
	@Getter
	protected final KoolSMPCore plugin = KoolSMPCore.getInstance();

	protected final String playerNotFound = "<gray>Could not find the specified player on the server (are they online?)";
	protected final String playersOnly = "<red>This command can only be executed in-game.";

	protected KoolCommand()
	{
		super("temporary", "temporary", "temporary", new ArrayList<>());

		if (!getClass().isAnnotationPresent(CommandParameters.class))
		{
			throw new IllegalStateException("Commands of this type require the CommandParameters annotation");
		}

		final CommandParameters parameters = Objects.requireNonNull(getClass().getAnnotation(CommandParameters.class));

		setName(parameters.name());
		setDescription(parameters.description());
		setUsage(parameters.usage());
		setPermission(parameters.permission().isBlank() ? "kfc.command." + getName() : parameters.permission());
		setAliases(Arrays.stream(parameters.aliases()).toList());
	}

	@Override
	public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args)
	{
		// Plugin is disabled, lmao
		if (plugin == null || !plugin.isEnabled())
		{
			return true;
		}

		if (!testPermissionSilent(sender))
		{
			msg(sender, "<red>You don't have permission to run this command.");
			return true;
		}

		try
		{
			if (!run(sender, sender instanceof Player player ? player : null, this, commandLabel, args))
			{
				msg(sender, "<gray>Usage: <white><usage></white>", Placeholder.component("usage",
						FUtil.miniMessage(getUsage(), Placeholder.unparsed("command", commandLabel))));
				return false;
			}
		}
		catch (Throwable ex)
		{
			msg(sender, Component.text(ex.getMessage() != null ? ex.getMessage() : ex.getClass().getName()).color(NamedTextColor.RED));
			FLog.error("Command error", ex);
		}

		return true;
	}

	@Override
	public final @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) throws IllegalArgumentException
	{
		List<String> tabCompletions = List.of();

		if (plugin == null || !plugin.isEnabled() || !testPermissionSilent(sender))
		{
			return tabCompletions;
		}

		try
		{
			if (args.length > 0)
			{
				tabCompletions = tabComplete(sender, this, commandLabel, args);
			}
		}
		catch (Throwable ex)
		{
			tabCompletions = List.of();
			FLog.error("An error occurred while attempting to tab complete command '" + getName() + "'", ex);
			msg(sender, "<red>Tab completion error: <message>",
					Placeholder.unparsed("message", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getName()));
		}

		// The tab completer returned null
		if (tabCompletions == null)
		{
			tabCompletions = super.tabComplete(sender, commandLabel, args);
		}

		return tabCompletions.stream().filter(entry -> entry.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).toList();
	}

	public @Nullable List<String> tabComplete(CommandSender sender, Command command, String commandLabel, String[] args)
	{
		return List.of();
	}

	public abstract boolean run(CommandSender sender, Player playerSender, Command command, String commandLabel, String[] args);

	/**
	 * Send a MiniMessage-formatted message to the provided CommandSender.
	 * @param sender		CommandSender
	 * @param message		String
	 * @param placeholders	TagResolver[]
	 */
	protected final void msg(CommandSender sender, String message, TagResolver... placeholders)
	{
		sender.sendRichMessage(message, placeholders);
	}

	/**
	 * Send an Adventure message to the provided CommandSender.
	 * @param sender		CommandSender
	 * @param message		Component
	 */
	protected final void msg(CommandSender sender, Component message)
	{
		sender.sendMessage(message);
	}

	/**
	 * Broadcast an Adventure message to the server.
	 * @param message		Component
	 */
	protected final void broadcast(Component message)
	{
		FUtil.broadcast(message);
	}

	/**
	 * Broadcast an Adventure message to everyone with a specific permission.
	 * @param message		Component
	 */
	protected final void broadcast(Component message, String permission)
	{
		FUtil.broadcast(message, permission);
	}

	/**
	 * Broadcast an MiniMessage-formatted message to the server.
	 * @param message		String
	 * @param placeholders	TagResolver[]
	 */
	protected final void broadcast(String message, TagResolver... placeholders)
	{
		FUtil.broadcast(message, placeholders);
	}

	/**
	 * Broadcast an MiniMessage-formatted message to everyone with a specific permission.
	 * @param message		String
	 * @param placeholders	TagResolver[]
	 */
	protected final void broadcast(String permission, String message, TagResolver... placeholders)
	{
		FUtil.broadcast(permission, message, placeholders);
	}
}
