package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.config.ConfigEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

@CommandParameters(name = "commandspy", description = "Spy on other people's commands", aliases = {"cmdspy", "cspy"})
public class CommandSpyCommand extends KoolCommand implements Listener
{
    private final NamespacedKey commandSpyKey = new NamespacedKey(plugin, "commandspy");

    public CommandSpyCommand()
    {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args)
    {
        if (playerSender == null)
        {
            msg(sender, playersOnly);
            return true;
        }

        PersistentDataContainer container = playerSender.getPersistentDataContainer();

        if (!container.getOrDefault(commandSpyKey, PersistentDataType.BOOLEAN, false))
        {
            msg(sender, "<gray>CommandSpy <green>enabled</green>.");
            container.set(commandSpyKey, PersistentDataType.BOOLEAN, true);
        }
        else
        {
            msg(sender, "<gray>CommandSpy <red>disabled</red>.");
            container.set(commandSpyKey, PersistentDataType.BOOLEAN, false);
        }

        return true;
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event)
    {
        Player sender = event.getPlayer();
        String commandMessage = event.getMessage();

        Bukkit.getOnlinePlayers().stream().filter(player -> player.hasPermission(Objects.requireNonNull(getPermission())))
                .filter(player -> player.getPersistentDataContainer().getOrDefault(commandSpyKey, PersistentDataType.BOOLEAN, false))
                .filter(player -> !sender.equals(player))
                .forEach(player -> msg(player, ConfigEntry.FORMATS_COMMANDSPY.getString(),
                        Placeholder.parsed("name", sender.getName()),
                        Placeholder.parsed("raw_command", commandMessage),
                        Placeholder.component("command", Component.text(commandMessage))));
    }
}
