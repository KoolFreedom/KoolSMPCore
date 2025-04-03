package eu.koolfreedom.command.impl;

import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.command.FreedomCommand;
import eu.koolfreedom.command.impl.Messages;
import eu.koolfreedom.log.FLog;
import eu.koolfreedom.util.KoolSMPCoreBase;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import static eu.koolfreedom.KoolSMPCore.main;

public class KoolSMPCoreCommand extends FreedomCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length == 0)
        {
            KoolSMPCore.BuildProperties build = KoolSMPCore.build;
            sender.sendMessage(main.mmDeserialize("<gold>KoolSMPCore - A multi-purpose SMP Core plugin."));
            sender.sendMessage(main.mmDeserialize("<gold>Version <blue>" + String.format("%s.%s", build.version, build.number)));
            sender.sendMessage(main.mmDeserialize("<gold>Compiled on <blue>" + String.format("%s" + " <gold>by <blue>" + "%s", build.date, build.author)));
            sender.sendMessage(main.mmDeserialize("<green>Visit <aqua>https://github.com/KoolFreedom/KoolSMPCore <green>for more information."));
            return true;
        }
        if (args[0].equalsIgnoreCase("reload"))
        {
            if (!sender.hasPermission("kf.senior"))
            {
                sender.sendMessage(Messages.MSG_NO_PERMS);
                return true;
            }
            try
            {
                main.reloadConfig();
                sender.sendMessage(Messages.RELOADED);
                return true;
            }
            catch (Exception ex)
            {
                FLog.severe(ex.toString());
                sender.sendMessage(Messages.FAILED);
            }
            return true;
        }
        return false;
    }

    public List<String> onTabComplete(CommandSender sender, @NotNull Command command, @NotNull String label, String[] args)
    {
        if (sender.hasPermission("kf.senior"))
        {
            return Collections.singletonList("reload");
        }
        else
        {
            return null;
        }
    }
}
