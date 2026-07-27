package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.config.MainConfig;
import eu.koolfreedom.util.FLog;
import eu.koolfreedom.util.BuildProperties;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@CommandParameters(name = "koolsmpcore", description = "Display information about the plugin or reload it.",
        usage = "/<command> [reload]")
public class KoolSMPCoreCommand extends KoolCommand
{
    private static final String DIVIDER = "<dark_gray><strikethrough>                                        ";

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args)
    {
        if (args.length == 0 || !sender.hasPermission("kfc.command.koolsmpcore.reload"))
        {
            sendInfo(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload"))
        {
            try
            {
                MainConfig.load();
                plugin.getGroupManager().loadGroups();
                plugin.getChatListener().loadFilters();
                plugin.resetAnnouncer();
                msg(sender, "<green>The configuration file has been reloaded.");
            }
            catch (Exception ex)
            {
                FLog.error("Failed to load configuration", ex);
                msg(sender, "<red>An error occurred whilst attempting to reload the configuration.");
            }

            return true;
        }

        if (args[0].equalsIgnoreCase("update"))
        {
            if (!sender.hasPermission("kfc.command.koolsmpcore.update"))
            {
                msg(sender, "<red>You don't have permission to do this.");
                return true;
            }
            msg(sender, "<gray>Checking for updates...");
            plugin.getUpdateChecker().checkAndUpdate(sender);
            return true;
        }

        return false;
    }

    private void sendInfo(CommandSender sender)
    {
        String authors = String.join(", ", plugin.getPluginMeta().getAuthors());

        msg(sender, DIVIDER);
        msg(sender, "<gold><b>KoolSMPCore</b> <gray>- The Core plugin of KoolFreedomSMP");
        msg(sender, "<dark_gray> » <gray>Version <white><version>",
                Placeholder.unparsed("version", plugin.getBuildMeta().getVersion()));
        msg(sender, "<dark_gray> » <gray>Compiled on <white><date></white> by <white><builder></white>.",
                Placeholder.unparsed("date", plugin.getBuildMeta().getDate()),
                Placeholder.unparsed("builder", plugin.getBuildMeta().getAuthor()));
        msg(sender, "<dark_gray> » <gray>Authors <white><authors>",
                Placeholder.unparsed("authors", authors));
        msg(sender, DIVIDER);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, @NotNull Command command, @NotNull String label, String[] args)
    {
        if (args.length == 1)
        {
            List<String> completions = new ArrayList<>();
            if (sender.hasPermission("kfc.command.koolsmpcore.reload")) completions.add("reload");
            if (sender.hasPermission("kfc.command.koolsmpcore.update")) completions.add("update");
            return completions;
        }
        return List.of();
    }
}
