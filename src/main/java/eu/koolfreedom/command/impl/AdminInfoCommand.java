package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.config.ConfigEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

@CommandParameters(name = "admininfo", description = "Displays info about applying for staff", aliases = {"ai", "staffinfo",})
public class AdminInfoCommand extends KoolCommand
{
    private static final List<Component> ADMIN_INFO = ConfigEntry.ADMININFO.getStringList()
            .stream().map(info -> MiniMessage.miniMessage().deserialize(info)).toList();

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String label, String[] args)
    {
        if (ADMIN_INFO.isEmpty())
        {
            msg(sender, "<red>There is currently nothing configured for config section 'admininfo', contact the server's administrator to resolve this error.");
            return true;
        }

        ADMIN_INFO.forEach(component -> msg(sender, component));
        return true;
    }
}
