package eu.koolfreedom.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.util.FLog;
import eu.koolfreedom.util.FUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * <h1>KoolCommand</h1>
 * <p>The foundation of all commands in KoolSMPCore. Commands are currently registered in the plugin's onEnable method
 * 	through the {@link CommandLoader}.</p>
 */
public abstract class KoolCommand
{
	@Getter
	protected static final KoolSMPCore plugin = KoolSMPCore.getInstance();

	protected final String playersOnly = "<red>This command can only be executed in-game.";
	protected final String noPermission = "<red>You do not have permission to execute this command!";
	protected final String noReasonProvided = "<gray>Must provide a reason.";
	protected final String playerNotFound = "<gray>Could not find specified player";

	private final CommandParameters parameters;

	protected KoolCommand()
	{
		if (!getClass().isAnnotationPresent(CommandParameters.class))
		{
			throw new IllegalStateException("Commands must be annotated with @CommandParameters");
		}

		this.parameters = Objects.requireNonNull(getClass().getAnnotation(CommandParameters.class));
	}

	public final String getName()
	{
		return parameters.name();
	}

	public final String getDescription()
	{
		return parameters.description();
	}

	public final List<String> getAliases()
	{
		return List.of(parameters.aliases());
	}

	public final String getPermissionNode()
	{
		return parameters.permission().isBlank() ? "kfc." + getName() : parameters.permission();
	}

	public boolean canUse(CommandSender sender)
	{
		return sender.hasPermission(getPermissionNode());
	}

	public abstract void build(LiteralArgumentBuilder<CommandSourceStack> root);

	protected static LiteralArgumentBuilder<CommandSourceStack> literal(String name)
	{
		return Commands.literal(name);
	}

	protected static <T> RequiredArgumentBuilder<CommandSourceStack, T> argument(String name, ArgumentType<T> type)
	{
		return Commands.argument(name, type);
	}

	/**
	 * A {@link Command} body that doesn't need to return the Brigadier success code itself - {@link #executes}
	 * supplies that, along with the permission check and error handling every command previously duplicated.
	 */
	@FunctionalInterface
	protected interface FallibleCommand
	{
		void run(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException;
	}

	/**
	 * Wraps a command body with the same permission re-check, disabled-plugin guard, and catch-all error handling
	 * the old {@code BasicCommand#execute} performed. {@link CommandSyntaxException} is left to propagate - that's
	 * Brigadier's own mechanism for showing a red error to the sender, so don't swallow it.
	 */
	protected final Command<CommandSourceStack> executes(FallibleCommand logic)
	{
		return ctx ->
		{
			CommandSender sender = ctx.getSource().getSender();

			if (plugin == null || !plugin.isEnabled())
			{
				return 0;
			}

			if (!canUse(sender))
			{
				msg(sender, noPermission);
				return 0;
			}

			try
			{
				logic.run(ctx);
				return Command.SINGLE_SUCCESS;
			}
			catch (CommandSyntaxException ex)
			{
				throw ex;
			}
			catch (Throwable ex)
			{
				msg(sender, "<red>Command execution error: <error>", Placeholder.unparsed("error", ex.toString()));
				FLog.error("Command error in {}", getName(), ex);
				return 0;
			}
		};
	}

	/**
	 * Resolves a single-player argument registered with {@code ArgumentTypes.player()}. Throws
	 * {@link CommandSyntaxException} - rendered to the sender automatically by Brigadier - if the selector matched
	 * no one currently online.
	 */
	protected static Player player(CommandContext<CommandSourceStack> ctx, String argName) throws CommandSyntaxException
	{
		return ctx.getArgument(argName, PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst();
	}

	protected static CommandSender sender(CommandContext<CommandSourceStack> ctx)
	{
		return ctx.getSource().getSender();
	}

	protected static @Nullable Player playerSender(CommandContext<CommandSourceStack> ctx)
	{
		return sender(ctx) instanceof Player player ? player : null;
	}

	protected final void msg(CommandSender sender, String message, TagResolver... placeholders)
	{
		sender.sendRichMessage(message, placeholders);
	}

	protected final void msg(CommandSender sender, Component message)
	{
		sender.sendMessage(message);
	}

	protected final void broadcast(Component message)
	{
		FUtil.broadcast(message);
	}

	protected final void broadcast(Component message, String permission)
	{
		FUtil.broadcast(message, permission);
	}

	protected final void broadcast(String message, TagResolver... placeholders)
	{
		FUtil.broadcast(message, placeholders);
	}

	protected final void broadcast(String permission, String message, TagResolver... placeholders)
	{
		FUtil.broadcast(permission, message, placeholders);
	}

	protected boolean isConsole(CommandSender sender)
	{
		return !(sender instanceof Player);
	}
}
