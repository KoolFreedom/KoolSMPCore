package eu.koolfreedom.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;

public class PluginCommandWrapper extends Command {
    private final FreedomCommand executor;

    public PluginCommandWrapper(String name, FreedomCommand executor) {
        super(name);
        this.executor = executor;
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        return executor.onCommand(sender, this, commandLabel, args);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return super.tabComplete(sender, alias, args);
    }
}
