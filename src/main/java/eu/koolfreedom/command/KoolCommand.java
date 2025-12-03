package eu.koolfreedom.command;

import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.util.FLog;
import eu.koolfreedom.util.FUtil;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * <h1>KoolCommand</h1>
 * <p>The foundation of all commands in KoolSMPCore. Commands are currently registered in the plugin's onEnable method
 * 	through the {@link CommandLoader}.</p>
 */
public abstract class KoolCommand extends Command implements PluginIdentifiableCommand
{
	@Getter
	protected final KoolSMPCore plugin = KoolSMPCore.getInstance();

	protected final String playerNotFound = "<gray>Could not find the specified player.";
	protected final String playersOnly = "<red>This command can only be executed in-game.";

	/**
	 * Constructor to initialize commands. Commands created this way <b>must</b> be annotated with the
	 * 	{@link CommandParameters} annotations to be accepted, or else an {@link IllegalStateException} is thrown.
	 */
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
	public final boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args)
	{
		// Plugin is disabled, lmao
		if (plugin == null || !plugin.isEnabled())
		{
			return true;
		}

		// Make sure they have permission and if they don't, refuse to run the command.
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
			msg(sender, "<red>Command execution error: <error>", Placeholder.unparsed("error", ex.toString()));
			FLog.error("Command error", ex);
		}

		return true;
	}

	@Override
	public final @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String commandLabel,
												   @NotNull String[] args) throws IllegalArgumentException
	{
		List<String> tabCompletions = List.of();

		// Return an empty list of tab completions if the plugin isn't available, the sender doesn't have permission
		// 	to run the command, or args is empty
		if (plugin == null || !plugin.isEnabled() || !testPermissionSilent(sender) || args.length == 0)
		{
			return tabCompletions;
		}

		try
		{
			tabCompletions = tabComplete(sender, this, commandLabel, args);
		}
		catch (Throwable ex)
		{
			tabCompletions = List.of();
			FLog.error("An error occurred while attempting to tab complete command '" + getName() + "'", ex);
			msg(sender, "<red>Tab completion error: <message>", Placeholder.unparsed("message", ex.toString()));
		}

		// The tab completer returned null, so fallback to regular
		if (tabCompletions == null)
		{
			tabCompletions = super.tabComplete(sender, commandLabel, args);
		}

		// Remove irrelevant options and only show the ones matching what we have in store.
		return tabCompletions.stream().filter(entry -> entry.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).toList();
	}

	/**
	 * Gets tab completions for the command as the sender based on the given arguments. This method is wrapped by
	 * 	{@link #tabComplete(CommandSender, String, String[])} to handle permission checks and then tab completes the
	 * 	command while also catching any possible exceptions that might be thrown by a particular command. While it does
	 * 	tab complete everything, you should still catch any cases of exceptions caused by user errors.
	 * @param sender		The {@link CommandSender} tab completing the command
	 * @param command		The {@link Command} object.
	 * @param commandLabel	The alias the sender chose to use to run the command.
	 * @param args          An array of strings used for the command arguments.
	 * @return				A list of Strings containing possible arguments. The aforementioned wrapper method will
	 * 						suggest any arguments provided that start with the input given by the user. If returning
	 * 						null, the plugin will fallback to the default way of tab completions, which is to just throw
	 * 						a list of users. To return no tab completions, return an empty list instead using something
	 * 						like {@link List#of()} or {@link Collections#emptyList()}.
	 */
	public @Nullable List<String> tabComplete(CommandSender sender, Command command, String commandLabel, String[] args)
	{
		return List.of();
	}

	/**
	 * Executes the command as the sender. This method is wrapped by {@link #execute(CommandSender, String, String[])}
	 * 	to handle permission checks and then run the command while also catching any possible exceptions that might be
	 * 	thrown by a particular command. While it does catch everything, you should still catch any cases of exceptions
	 * 	caused by user error (i.e., a NumberFormatException being thrown).
	 * @param sender		The {@link CommandSender} running the command
	 * @param playerSender	The sender represented as a {@link Player} if they are one, otherwise it returns null. This
	 *                      behavior is great for when you want to exclude sources that aren't in-game (like console)
	 *                      from performing a specific task or running the command.
	 * @param command		The {@link Command} object.
	 * @param commandLabel 	The alias the sender chose to use to run the command.
	 * @param args			An array of strings used for the command arguments.
	 * @return				True if the user provided the correct arguments. If this returns false, a usage message will
	 * 						be shown to the sender.
	 */
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
	 * Broadcast a MiniMessage-formatted message to the server.
	 * @param message		String
	 * @param placeholders	TagResolver[]
	 */
	protected final void broadcast(String message, TagResolver... placeholders)
	{
		FUtil.broadcast(message, placeholders);
	}

	/**
	 * Broadcast a MiniMessage-formatted message to everyone with a specific permission.
	 * @param message		String
	 * @param placeholders	TagResolver[]
	 */
	protected final void broadcast(String permission, String message, TagResolver... placeholders)
	{
		FUtil.broadcast(permission, message, placeholders);
	}
}
