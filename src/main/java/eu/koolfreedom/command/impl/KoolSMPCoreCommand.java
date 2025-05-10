package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.config.MainConfig;
import eu.koolfreedom.log.FLog;
import eu.koolfreedom.util.BuildProperties;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

@CommandParameters(name = "koolsmpcore", description = "Display information about the plugin or reload it.",
        usage = "/<command> [reload]", aliases = "kfc")
public class KoolSMPCoreCommand extends KoolCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args)
    {
        if (args.length == 0 || !sender.hasPermission("kfc.command.koolsmpcore.reload"))
        {
            BuildProperties build = plugin.buildMeta;
            msg(sender, "<white><b>KoolSMPCore - The Core of KoolFreedom SMP.");
            msg(sender, "<gray>Version <white><version>.<build>", Placeholder.unparsed("version", build.getVersion()),
                    Placeholder.unparsed("build", build.getNumber()));
            msg(sender, "<gray>Compiled on <white><date></white> by <white><builder></white>.",
                    Placeholder.unparsed("date", build.getDate()),
                    Placeholder.unparsed("builder", build.getAuthor()));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload"))
        {
            try
            {
                MainConfig.load();
                plugin.groupCosmetics.loadGroups();
                plugin.chatFilter.load();
                msg(sender, "<green>The configuration file has been reloaded.");
            }
            catch (Exception ex)
            {
                FLog.error("Failed to load configuration", ex);
                msg(sender, "<red>An error occurred whilst attempting to reload the configuration.");
            }

            return true;
        }

        return false;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, @NotNull Command command, @NotNull String label, String[] args)
    {
        if (sender.hasPermission("kfc.command.koolsmpcore.reload") && args.length == 1)
        {
            return Collections.singletonList("reload");
        }
        else
        {
            return List.of();
        }
    }
}
