package eu.koolfreedom.command;

import eu.koolfreedom.log.FLog;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.reflections.Reflections;

import java.util.ArrayList;
import java.util.Arrays;
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

	public void loadCommands()
	{
		new Reflections(path).getSubTypesOf(KoolCommand.class).forEach(commandClass ->
		{
			try
			{
				final KoolCommand command = commandClass.getDeclaredConstructor().newInstance();
				Bukkit.getCommandMap().register(command.getName(), "koolsmpcore", command);
				koolCommands.add(command);
			}
			catch (Throwable ex)
			{
				FLog.error("Failed to load command {}", commandClass.getName(), ex);
			}
		});
	}
}
