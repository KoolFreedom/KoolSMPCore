package eu.koolfreedom.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import eu.koolfreedom.util.FLog;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.reflections.Reflections;

import java.util.ArrayList;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class CommandLoader
{
	private final List<KoolCommand> koolCommands = new ArrayList<>();

	private final String path;

	public CommandLoader(Class<? extends KoolCommand> sacrifice)
	{
		this.path = sacrifice.getPackage().getName();
	}

	/**
	 * Discovers, builds, and registers every {@link KoolCommand} subclass under {@link #path}.
	 * @param commands	The {@link Commands} registrar, obtained from a {@code LifecycleEvents.COMMANDS} event.
	 */
	public void loadCommands(Commands commands)
	{
		new Reflections(path).getSubTypesOf(KoolCommand.class).forEach(commandClass ->
		{
			try
			{
				KoolCommand command = commandClass.getDeclaredConstructor().newInstance();

				LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(command.getName())
						.requires(source -> command.canUse(source.getSender()));

				command.build(root);

				LiteralCommandNode<CommandSourceStack> node = root.build();
				commands.register(node, command.getDescription(), command.getAliases());
				koolCommands.add(command);
			}
			catch (Throwable ex)
			{
				FLog.error("Failed to load command {}", commandClass.getName(), ex);
			}
		});
	}
}